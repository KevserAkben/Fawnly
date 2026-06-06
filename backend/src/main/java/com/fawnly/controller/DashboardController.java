package com.fawnly.controller;

import com.fawnly.dto.scan.ScanResponse;
import com.fawnly.security.UserPrincipal;
import com.fawnly.service.ScanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Dashboard summary endpoints")
public class DashboardController {

    private final ScanService scanService;

    public DashboardController(ScanService scanService) {
        this.scanService = scanService;
    }

    @GetMapping
    @Operation(summary = "Get dashboard summary")
    public ResponseEntity<Map<String, Object>> getDashboard(@AuthenticationPrincipal UserPrincipal principal) {
        List<ScanResponse> scans = scanService.getScanHistory(principal.getId());

        long totalScans = scans.size();
        long completedScans = scans.stream().filter(s -> "done".equals(s.getStatus())).count();
        long totalFindings = scans.stream().mapToLong(ScanResponse::getFindingCount).sum();
        long failedScans = scans.stream().filter(s -> "failed".equals(s.getStatus())).count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalScans", totalScans);
        summary.put("completedScans", completedScans);
        summary.put("failedScans", failedScans);
        summary.put("totalFindings", totalFindings);
        summary.put("recentScans", scans.stream().limit(5).toList());
        summary.put("username", principal.getUsername());

        return ResponseEntity.ok(summary);
    }
}
