package com.fawnly.service;

import com.fawnly.entity.Finding;
import com.fawnly.entity.Scan;
import com.fawnly.repository.FindingRepository;
import com.fawnly.repository.ScanRepository;
import com.fawnly.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.*;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ScanExecutorService {

    private static final Logger log = LoggerFactory.getLogger(ScanExecutorService.class);
    private static final int GIT_TIMEOUT_MINUTES = 3;
    private static final long MAX_EXTRACTED_BYTES = 100L * 1024 * 1024;
    private static final int MAX_ZIP_ENTRIES = 5000;
    private static final int ERROR_MESSAGE_MAX = 500;

    private final ScanRepository scanRepository;
    private final FindingRepository findingRepository;
    private final SemgrepService semgrepService;

    @Value("${fawnly.scan.temp-dir:/tmp/sast-scans}")
    private String tempDir;

    public ScanExecutorService(ScanRepository scanRepository,
                               FindingRepository findingRepository,
                               SemgrepService semgrepService) {
        this.scanRepository = scanRepository;
        this.findingRepository = findingRepository;
        this.semgrepService = semgrepService;
    }

    @Async("scanTaskExecutor")
    public void executeScan(Long scanId) {
        Scan scan = waitForScan(scanId);
        if (scan == null) {
            log.error("Scan {} not found after retries, aborting", scanId);
            return;
        }

        Path scanDir = Paths.get(tempDir, String.valueOf(scanId));
        try {
            scan.setStatus("running");
            scan.setStartedAt(Instant.now());
            scanRepository.save(scan);

            Files.createDirectories(scanDir);
            Path sourceDir = prepareSource(scan, scanDir);

            List<Finding> findings = semgrepService.runScan(scanId, sourceDir);
            findingRepository.saveAll(findings);

            scan.setStatus("done");
            scan.setFinishedAt(Instant.now());
            scan.setErrorMessage(null);
            scanRepository.save(scan);

            log.info("Scan {} completed with {} findings", scanId, findings.size());
        } catch (Exception e) {
            log.error("Scan {} failed", scanId, e);
            try {
                scan.setStatus("failed");
                scan.setFinishedAt(Instant.now());
                scan.setErrorMessage(truncate(e.getMessage(), ERROR_MESSAGE_MAX));
                scanRepository.save(scan);
            } catch (Exception persistError) {
                log.error("Failed to persist error status for scan {}", scanId, persistError);
            }
        } finally {
            cleanupDirectory(scanDir);
        }
    }

    private Scan waitForScan(Long scanId) {
        for (int attempt = 0; attempt < 5; attempt++) {
            Scan scan = scanRepository.findById(scanId).orElse(null);
            if (scan != null) {
                return scan;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private Path prepareSource(Scan scan, Path scanDir) throws Exception {
        if ("git".equals(scan.getSourceType())) {
            return cloneRepository(scan.getSourceRef(), scanDir);
        }
        return unzipSource(scan.getSourceRef(), scanDir);
    }

    private Path cloneRepository(String githubUrl, Path scanDir) throws Exception {
        String normalizedUrl = ValidationUtil.normalizeGithubUrl(githubUrl);
        if (normalizedUrl == null) {
            throw new RuntimeException("Invalid GitHub URL");
        }

        Path targetDir = scanDir.resolve("source");
        Files.createDirectories(targetDir);

        ProcessBuilder pb = new ProcessBuilder(
                "git", "clone", "--depth", "1", "--single-branch", "--no-tags",
                "--", normalizedUrl, targetDir.toString());
        pb.redirectErrorStream(true);
        pb.environment().put("GIT_TERMINAL_PROMPT", "0");
        pb.environment().put("GIT_ASKPASS", "echo");

        Path gitLog = scanDir.resolve("git.log");
        pb.redirectOutput(gitLog.toFile());

        Process process = pb.start();
        boolean finished = process.waitFor(GIT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Git clone timed out");
        }
        if (process.exitValue() != 0) {
            String output = Files.exists(gitLog) ? Files.readString(gitLog) : "";
            throw new RuntimeException("Git clone failed: " + truncate(output, 300));
        }

        return targetDir;
    }

    private Path unzipSource(String zipPath, Path scanDir) throws Exception {
        Path zipFile = Paths.get(zipPath).toAbsolutePath().normalize();
        Path allowedRoot = Paths.get(tempDir).toAbsolutePath().normalize();
        if (!zipFile.startsWith(allowedRoot) || !Files.exists(zipFile)) {
            throw new RuntimeException("Invalid ZIP path");
        }

        Path targetDir = scanDir.resolve("source").toAbsolutePath().normalize();
        Files.createDirectories(targetDir);

        long extractedBytes = 0;
        int entryCount = 0;

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_ZIP_ENTRIES) {
                    throw new RuntimeException("ZIP contains too many entries");
                }

                String name = entry.getName();
                if (name.contains("\0")) {
                    throw new RuntimeException("Invalid zip entry name");
                }

                Path entryPath = targetDir.resolve(name).normalize();
                if (!entryPath.startsWith(targetDir)) {
                    throw new RuntimeException("Zip entry outside target directory");
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (var out = Files.newOutputStream(entryPath)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = zis.read(buffer)) != -1) {
                            extractedBytes += read;
                            if (extractedBytes > MAX_EXTRACTED_BYTES) {
                                throw new RuntimeException("Unzipped content exceeds size limit");
                            }
                            out.write(buffer, 0, read);
                        }
                    }
                }
                zis.closeEntry();
            }
        }

        if (entryCount == 0) {
            throw new RuntimeException("ZIP file is empty or invalid");
        }

        Files.deleteIfExists(zipFile);
        return targetDir;
    }

    public void cleanupDirectory(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (var walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception e) {
                            log.warn("Failed to delete {}", path, e);
                        }
                    });
        } catch (Exception e) {
            log.warn("Failed to cleanup directory {}", directory, e);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "Unknown error";
        }
        String cleaned = value.replaceAll("[\\r\\n]+", " ").trim();
        return cleaned.length() <= max ? cleaned : cleaned.substring(0, max);
    }
}
