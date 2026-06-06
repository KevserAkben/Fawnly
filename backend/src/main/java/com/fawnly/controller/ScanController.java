package com.fawnly.controller;

import com.fawnly.dto.scan.ScanResponse;
import com.fawnly.dto.scan.ScanStatusResponse;
import com.fawnly.dto.scan.StartScanRequest;
import com.fawnly.security.UserPrincipal;
import com.fawnly.service.ScanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/scans")
@Tag(name = "Scans", description = "Security scan management endpoints")
public class ScanController {

    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    @PostMapping("/git")
    @Operation(summary = "Start a Git repository scan")
    public ResponseEntity<ScanResponse> startGitScan(@AuthenticationPrincipal UserPrincipal principal,
                                                     @Valid @RequestBody StartScanRequest request) {
        ScanResponse response = scanService.startGitScan(
                principal.getId(), principal.getUsername(), request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/zip")
    @Operation(summary = "Start a ZIP file scan")
    public ResponseEntity<ScanResponse> startZipScan(@AuthenticationPrincipal UserPrincipal principal,
                                                     @RequestParam("projectName") String projectName,
                                                     @RequestParam("file") MultipartFile file) {
        ScanResponse response = scanService.startZipScan(
                principal.getId(), principal.getUsername(), projectName, file);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Get scan status for polling")
    public ResponseEntity<ScanStatusResponse> getScanStatus(@AuthenticationPrincipal UserPrincipal principal,
                                                              @PathVariable Long id) {
        return ResponseEntity.ok(scanService.getScanStatus(principal.getId(), id));
    }

    @GetMapping
    @Operation(summary = "Get scan history")
    public ResponseEntity<List<ScanResponse>> getScanHistory(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(scanService.getScanHistory(principal.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get scan details")
    public ResponseEntity<ScanResponse> getScan(@AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable Long id) {
        return ResponseEntity.ok(scanService.getScan(principal.getId(), id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a scan and its findings")
    public ResponseEntity<Void> deleteScan(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable Long id) {
        scanService.deleteScan(principal.getId(), principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
