package com.fawnly.service;

import com.fawnly.dto.common.MessageResponse;
import com.fawnly.dto.settings.ChangePasswordRequest;
import com.fawnly.dto.settings.SessionResponse;
import com.fawnly.dto.settings.UpdateUsernameRequest;
import com.fawnly.entity.EmailVerification;
import com.fawnly.entity.User;
import com.fawnly.exception.BadRequestException;
import com.fawnly.exception.ResourceNotFoundException;
import com.fawnly.repository.EmailVerificationRepository;
import com.fawnly.repository.SessionRepository;
import com.fawnly.repository.UserRepository;
import com.fawnly.security.UserPrincipal;
import com.fawnly.util.OtpGenerator;
import com.fawnly.util.SanitizeUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SettingsService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    public SettingsService(UserRepository userRepository,
                           SessionRepository sessionRepository,
                           EmailVerificationRepository emailVerificationRepository,
                           PasswordEncoder passwordEncoder,
                           EmailService emailService,
                           AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public MessageResponse updateUsername(UserPrincipal principal, UpdateUsernameRequest request) {
        String newUsername = SanitizeUtil.sanitize(request.getUsername());

        if (userRepository.existsByUsernameAndIdNot(newUsername, principal.getId())) {
            throw new BadRequestException("Username already taken");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String oldUsername = user.getUsername();
        user.setUsername(newUsername);
        userRepository.save(user);

        auditLogService.log(principal.getId(), newUsername, "USERNAME_CHANGED",
                "Username changed from " + oldUsername + " to " + newUsername);
        return new MessageResponse("Username updated successfully");
    }

    @Transactional
    public MessageResponse requestPasswordChangeOtp(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String code = OtpGenerator.generate();
        emailVerificationRepository.invalidateUnused(user.getId(), AuthService.PURPOSE_PASSWORD_CHANGE);

        EmailVerification verification = new EmailVerification();
        verification.setUserId(user.getId());
        verification.setCode(code);
        verification.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        verification.setPurpose(AuthService.PURPOSE_PASSWORD_CHANGE);
        emailVerificationRepository.save(verification);

        emailService.sendOtp(user.getEmail(), code, "password change");
        auditLogService.log(principal.getId(), principal.getUsername(), "PASSWORD_CHANGE_OTP_REQUESTED",
                "OTP sent for password change");

        return new MessageResponse("OTP sent to your email");
    }

    @Transactional
    public MessageResponse changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new BadRequestException("Passwords do not match");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        EmailVerification verification = emailVerificationRepository
                .findByUserIdAndCodeAndPurposeAndUsedFalse(
                        user.getId(), SanitizeUtil.sanitize(request.getCode()),
                        AuthService.PURPOSE_PASSWORD_CHANGE)
                .orElseThrow(() -> new BadRequestException("Invalid OTP code"));

        if (verification.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("OTP code has expired");
        }

        verification.setUsed(true);
        emailVerificationRepository.save(verification);

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        sessionRepository.deleteByUserId(user.getId());

        auditLogService.log(principal.getId(), principal.getUsername(), "PASSWORD_CHANGED",
                "Password changed, all sessions terminated");
        return new MessageResponse("Password changed successfully. Please log in again.");
    }

    public List<SessionResponse> getActiveSessions(UserPrincipal principal, String currentRefreshToken) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(principal.getId()).stream()
                .map(session -> {
                    SessionResponse response = new SessionResponse();
                    response.setId(session.getId());
                    response.setDeviceInfo(session.getDeviceInfo());
                    response.setCreatedAt(session.getCreatedAt());
                    response.setCurrent(session.getRefreshToken().equals(currentRefreshToken));
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse revokeSession(UserPrincipal principal, Long sessionId) {
        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (!session.getUserId().equals(principal.getId())) {
            throw new ResourceNotFoundException("Session not found");
        }

        sessionRepository.delete(session);

        auditLogService.log(principal.getId(), principal.getUsername(), "SESSION_REVOKED",
                "Session " + sessionId + " revoked");
        return new MessageResponse("Session revoked successfully");
    }
}
