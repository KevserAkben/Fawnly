package com.fawnly.service;

import com.fawnly.entity.Finding;
import com.fawnly.entity.Scan;
import com.fawnly.repository.FindingRepository;
import com.fawnly.repository.ScanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ScanExecutorService {

    private static final Logger log = LoggerFactory.getLogger(ScanExecutorService.class);

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
        Scan scan = scanRepository.findById(scanId).orElse(null);
        if (scan == null) {
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
            scanRepository.save(scan);

            log.info("Scan {} completed with {} findings", scanId, findings.size());
        } catch (Exception e) {
            log.error("Scan {} failed", scanId, e);
            scan.setStatus("failed");
            scan.setFinishedAt(Instant.now());
            scan.setErrorMessage(e.getMessage());
            scanRepository.save(scan);
        } finally {
            cleanupDirectory(scanDir);
        }
    }

    private Path prepareSource(Scan scan, Path scanDir) throws Exception {
        if ("git".equals(scan.getSourceType())) {
            return cloneRepository(scan.getSourceRef(), scanDir);
        } else {
            return unzipSource(scan.getSourceRef(), scanDir);
        }
    }

    private Path cloneRepository(String githubUrl, Path scanDir) throws Exception {
        Path targetDir = scanDir.resolve("source");
        Files.createDirectories(targetDir);

        ProcessBuilder pb = new ProcessBuilder("git", "clone", "--depth", "1", githubUrl, targetDir.toString());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Git clone failed: " + output);
        }

        return targetDir;
    }

    private Path unzipSource(String zipPath, Path scanDir) throws Exception {
        Path zipFile = Paths.get(zipPath);
        Path targetDir = scanDir.resolve("source");
        Files.createDirectories(targetDir);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(targetDir)) {
                    throw new RuntimeException("Zip entry outside target directory: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
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
}
