package com.fawnly.service;

import com.fawnly.dto.scan.ScanResponse;
import com.fawnly.dto.scan.ScanStatusResponse;
import com.fawnly.dto.scan.StartScanRequest;
import com.fawnly.entity.Scan;
import com.fawnly.exception.BadRequestException;
import com.fawnly.exception.ResourceNotFoundException;
import com.fawnly.repository.FindingRepository;
import com.fawnly.repository.ScanRepository;
import com.fawnly.util.SanitizeUtil;
import com.fawnly.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScanService {

    private final ScanRepository scanRepository;
    private final FindingRepository findingRepository;
    private final ScanExecutorService scanExecutorService;
    private final AuditLogService auditLogService;

    @Value("${fawnly.scan.temp-dir:/tmp/sast-scans}")
    private String tempDir;

    public ScanService(ScanRepository scanRepository,
                       FindingRepository findingRepository,
                       ScanExecutorService scanExecutorService,
                       AuditLogService auditLogService) {
        this.scanRepository = scanRepository;
        this.findingRepository = findingRepository;
        this.scanExecutorService = scanExecutorService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ScanResponse startGitScan(Long userId, String username, StartScanRequest request) {
        String projectName = SanitizeUtil.sanitize(request.getProjectName());
        String githubUrl = SanitizeUtil.sanitize(request.getGithubUrl());

        if (!"git".equals(request.getSourceType())) {
            throw new BadRequestException("Invalid source type for git scan");
        }
        if (!ValidationUtil.isValidGithubUrl(githubUrl)) {
            throw new BadRequestException("Invalid GitHub URL format");
        }

        Scan scan = createScan(userId, projectName, "git", githubUrl);
        auditLogService.log(userId, username, "SCAN_STARTED", "Git scan started for project: " + projectName);
        scanExecutorService.executeScan(scan.getId());
        return toResponse(scan);
    }

    @Transactional
    public ScanResponse startZipScan(Long userId, String username, String projectName, MultipartFile file) {
        String sanitizedName = SanitizeUtil.sanitize(projectName);

        if (sanitizedName == null || sanitizedName.isBlank()) {
            throw new BadRequestException("Project name is required");
        }
        if (sanitizedName.length() > 100) {
            throw new BadRequestException("Project name must be at most 100 characters");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("ZIP file is required");
        }
        if (!ValidationUtil.isValidZipFilename(file.getOriginalFilename())) {
            throw new BadRequestException("Only .zip files are allowed");
        }
        if (file.getSize() > 20 * 1024 * 1024) {
            throw new BadRequestException("ZIP file must not exceed 20 MB");
        }

        try {
            Scan scan = createScan(userId, sanitizedName, "zip", "pending");
            Path scanDir = Paths.get(tempDir, String.valueOf(scan.getId()));
            Files.createDirectories(scanDir);
            Path zipPath = scanDir.resolve("upload.zip");
            Files.copy(file.getInputStream(), zipPath, StandardCopyOption.REPLACE_EXISTING);

            scan.setSourceRef(zipPath.toString());
            scanRepository.save(scan);

            auditLogService.log(userId, username, "SCAN_STARTED", "ZIP scan started for project: " + sanitizedName);
            scanExecutorService.executeScan(scan.getId());
            return toResponse(scan);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to process ZIP file: " + e.getMessage());
        }
    }

    public ScanStatusResponse getScanStatus(Long userId, Long scanId) {
        Scan scan = scanRepository.findByIdAndUserId(scanId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Scan not found"));
        return new ScanStatusResponse(scan.getId(), scan.getStatus(), scan.getErrorMessage());
    }

    public List<ScanResponse> getScanHistory(Long userId) {
        return scanRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ScanResponse getScan(Long userId, Long scanId) {
        Scan scan = scanRepository.findByIdAndUserId(scanId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Scan not found"));
        return toResponse(scan);
    }

    @Transactional
    public void deleteScan(Long userId, String username, Long scanId) {
        Scan scan = scanRepository.findByIdAndUserId(scanId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Scan not found"));

        findingRepository.deleteByScanId(scanId);
        scanRepository.delete(scan);

        Path scanDir = Paths.get(tempDir, String.valueOf(scanId));
        scanExecutorService.cleanupDirectory(scanDir);

        auditLogService.log(userId, username, "SCAN_DELETED", "Scan deleted: " + scan.getProjectName());
    }

    private Scan createScan(Long userId, String projectName, String sourceType, String sourceRef) {
        Scan scan = new Scan();
        scan.setUserId(userId);
        scan.setProjectName(projectName);
        scan.setSourceType(sourceType);
        scan.setSourceRef(sourceRef);
        scan.setStatus("queued");
        return scanRepository.save(scan);
    }

    private ScanResponse toResponse(Scan scan) {
        ScanResponse response = new ScanResponse();
        response.setId(scan.getId());
        response.setProjectName(scan.getProjectName());
        response.setSourceType(scan.getSourceType());
        response.setSourceRef(scan.getSourceType().equals("zip") ? "[uploaded zip]" : scan.getSourceRef());
        response.setStatus(scan.getStatus());
        response.setStartedAt(scan.getStartedAt());
        response.setFinishedAt(scan.getFinishedAt());
        response.setCreatedAt(scan.getCreatedAt());
        response.setErrorMessage(scan.getErrorMessage());
        response.setFindingCount(findingRepository.countByScanId(scan.getId()));
        return response;
    }
}
