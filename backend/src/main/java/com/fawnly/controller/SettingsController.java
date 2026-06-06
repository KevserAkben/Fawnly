package com.fawnly.controller;

import com.fawnly.dto.common.MessageResponse;
import com.fawnly.dto.settings.ChangePasswordRequest;
import com.fawnly.dto.settings.SessionResponse;
import com.fawnly.dto.settings.UpdateUsernameRequest;
import com.fawnly.security.UserPrincipal;
import com.fawnly.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@Tag(name = "Settings", description = "User settings and session management")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @PutMapping("/username")
    @Operation(summary = "Update username")
    public ResponseEntity<MessageResponse> updateUsername(@AuthenticationPrincipal UserPrincipal principal,
                                                          @Valid @RequestBody UpdateUsernameRequest request) {
        return ResponseEntity.ok(settingsService.updateUsername(principal, request));
    }

    @PostMapping("/password/request-otp")
    @Operation(summary = "Request OTP for password change")
    public ResponseEntity<MessageResponse> requestPasswordOtp(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(settingsService.requestPasswordChangeOtp(principal));
    }

    @PutMapping("/password")
    @Operation(summary = "Change password with OTP verification")
    public ResponseEntity<MessageResponse> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                                          @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(settingsService.changePassword(principal, request));
    }

    @GetMapping("/sessions")
    @Operation(summary = "List active sessions")
    public ResponseEntity<List<SessionResponse>> getSessions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {
        return ResponseEntity.ok(settingsService.getActiveSessions(principal, refreshToken));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Revoke a session")
    public ResponseEntity<MessageResponse> revokeSession(@AuthenticationPrincipal UserPrincipal principal,
                                                           @PathVariable Long sessionId) {
        return ResponseEntity.ok(settingsService.revokeSession(principal, sessionId));
    }
}
