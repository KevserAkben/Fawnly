package com.fawnly.controller;

import com.fawnly.dto.finding.FindingResponse;
import com.fawnly.dto.finding.FindingSummaryResponse;
import com.fawnly.dto.finding.UpdateNoteRequest;
import com.fawnly.dto.finding.UpdateTriageRequest;
import com.fawnly.security.UserPrincipal;
import com.fawnly.service.FindingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scans/{scanId}/findings")
@Tag(name = "Findings", description = "Scan finding management endpoints")
public class FindingController {

    private final FindingService findingService;

    public FindingController(FindingService findingService) {
        this.findingService = findingService;
    }

    @GetMapping
    @Operation(summary = "Get all findings for a scan with summary")
    public ResponseEntity<FindingSummaryResponse> getFindings(@AuthenticationPrincipal UserPrincipal principal,
                                                              @PathVariable Long scanId) {
        return ResponseEntity.ok(findingService.getFindingsForScan(principal.getId(), scanId));
    }

    @PatchMapping("/{findingId}/triage")
    @Operation(summary = "Update finding triage status")
    public ResponseEntity<FindingResponse> updateTriage(@AuthenticationPrincipal UserPrincipal principal,
                                                        @PathVariable Long scanId,
                                                        @PathVariable Long findingId,
                                                        @Valid @RequestBody UpdateTriageRequest request) {
        return ResponseEntity.ok(findingService.updateTriage(
                principal.getId(), principal.getUsername(), scanId, findingId, request));
    }

    @PatchMapping("/{findingId}/note")
    @Operation(summary = "Update finding note")
    public ResponseEntity<FindingResponse> updateNote(@AuthenticationPrincipal UserPrincipal principal,
                                                      @PathVariable Long scanId,
                                                      @PathVariable Long findingId,
                                                      @Valid @RequestBody UpdateNoteRequest request) {
        return ResponseEntity.ok(findingService.updateNote(
                principal.getId(), principal.getUsername(), scanId, findingId, request));
    }
}
