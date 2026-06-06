package com.fawnly.service;

import com.fawnly.dto.finding.*;
import com.fawnly.entity.Finding;
import com.fawnly.entity.Scan;
import com.fawnly.exception.ResourceNotFoundException;
import com.fawnly.repository.FindingRepository;
import com.fawnly.repository.ScanRepository;
import com.fawnly.util.SanitizeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FindingService {

    private final FindingRepository findingRepository;
    private final ScanRepository scanRepository;
    private final AuditLogService auditLogService;

    public FindingService(FindingRepository findingRepository,
                          ScanRepository scanRepository,
                          AuditLogService auditLogService) {
        this.findingRepository = findingRepository;
        this.scanRepository = scanRepository;
        this.auditLogService = auditLogService;
    }

    public FindingSummaryResponse getFindingsForScan(Long userId, Long scanId) {
        Scan scan = scanRepository.findByIdAndUserId(scanId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Scan not found"));

        List<Finding> findings = findingRepository.findByScanIdOrderBySeverityAscLineNoAsc(scanId);

        FindingSummaryResponse summary = new FindingSummaryResponse();
        summary.setScanId(scanId);
        summary.setProjectName(scan.getProjectName());
        summary.setScanDate(scan.getFinishedAt() != null ? scan.getFinishedAt() : scan.getCreatedAt());
        summary.setTotal(findings.size());
        summary.setHigh(findings.stream().filter(f -> "HIGH".equals(f.getSeverity())).count());
        summary.setMedium(findings.stream().filter(f -> "MEDIUM".equals(f.getSeverity())).count());
        summary.setLow(findings.stream().filter(f -> "LOW".equals(f.getSeverity())).count());
        summary.setFindings(findings.stream().map(this::toResponse).collect(Collectors.toList()));
        return summary;
    }

    @Transactional
    public FindingResponse updateTriage(Long userId, String username, Long scanId, Long findingId,
                                        UpdateTriageRequest request) {
        verifyScanOwnership(userId, scanId);
        Finding finding = findingRepository.findByIdAndScanId(findingId, scanId)
                .orElseThrow(() -> new ResourceNotFoundException("Finding not found"));

        finding.setTriageStatus(SanitizeUtil.sanitize(request.getTriageStatus()));
        finding.setUpdatedAt(Instant.now());
        findingRepository.save(finding);

        auditLogService.log(userId, username, "FINDING_TRIAGE_UPDATED",
                "Finding " + findingId + " triage set to " + request.getTriageStatus());
        return toResponse(finding);
    }

    @Transactional
    public FindingResponse updateNote(Long userId, String username, Long scanId, Long findingId,
                                      UpdateNoteRequest request) {
        verifyScanOwnership(userId, scanId);
        Finding finding = findingRepository.findByIdAndScanId(findingId, scanId)
                .orElseThrow(() -> new ResourceNotFoundException("Finding not found"));

        finding.setNote(SanitizeUtil.sanitize(request.getNote()));
        finding.setUpdatedAt(Instant.now());
        findingRepository.save(finding);

        auditLogService.log(userId, username, "FINDING_NOTE_UPDATED", "Finding " + findingId + " note updated");
        return toResponse(finding);
    }

    private void verifyScanOwnership(Long userId, Long scanId) {
        scanRepository.findByIdAndUserId(scanId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Scan not found"));
    }

    private FindingResponse toResponse(Finding finding) {
        FindingResponse response = new FindingResponse();
        response.setId(finding.getId());
        response.setScanId(finding.getScanId());
        response.setRuleId(finding.getRuleId());
        response.setSeverity(finding.getSeverity());
        response.setFilePath(finding.getFilePath());
        response.setLineNo(finding.getLineNo());
        response.setOwaspCode(finding.getOwaspCode());
        response.setCwe(finding.getCwe());
        response.setMessage(finding.getMessage());
        response.setTriageStatus(finding.getTriageStatus());
        response.setNote(finding.getNote());
        response.setUpdatedAt(finding.getUpdatedAt());
        return response;
    }
}
