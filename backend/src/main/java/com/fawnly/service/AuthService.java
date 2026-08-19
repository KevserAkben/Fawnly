package com.fawnly.service;

import com.fawnly.dto.auth.*;
import com.fawnly.dto.common.MessageResponse;
import com.fawnly.entity.EmailVerification;
import com.fawnly.entity.Session;
import com.fawnly.entity.User;
import com.fawnly.exception.BadRequestException;
import com.fawnly.exception.UnauthorizedException;
import com.fawnly.repository.EmailVerificationRepository;
import com.fawnly.repository.SessionRepository;
import com.fawnly.repository.UserRepository;
import com.fawnly.security.JwtService;
import com.fawnly.security.UserPrincipal;
import com.fawnly.util.OtpGenerator;
import com.fawnly.util.SanitizeUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    public static final String PURPOSE_REGISTRATION = "REGISTRATION";
    public static final String PURPOSE_PASSWORD_CHANGE = "PASSWORD_CHANGE";

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       EmailVerificationRepository emailVerificationRepository,
                       SessionRepository sessionRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       EmailService emailService,
                       AuditLogService auditLogService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public MessageResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new BadRequestException("Passwords do not match");
        }

        String username = SanitizeUtil.sanitize(request.getUsername());
        String email = SanitizeUtil.sanitize(request.getEmail()).toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setActive(false);
        userRepository.save(user);

        sendVerificationCode(user, PURPOSE_REGISTRATION);

        auditLogService.log(user.getId(), username, "USER_REGISTER", "User registered, pending email verification");
        return new MessageResponse("Registration successful. Please verify your email with the OTP sent.");
    }

    @Transactional
    public TokenResponse verifyOtp(VerifyOtpRequest request) {
        String email = SanitizeUtil.sanitize(request.getEmail()).toLowerCase();
        String code = SanitizeUtil.sanitize(request.getCode());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid email or OTP code"));

        EmailVerification verification = emailVerificationRepository
                .findByUserIdAndCodeAndPurposeAndUsedFalse(user.getId(), code, PURPOSE_REGISTRATION)
                .orElseThrow(() -> new BadRequestException("Invalid email or OTP code"));

        if (verification.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("OTP code has expired");
        }

        verification.setUsed(true);
        emailVerificationRepository.save(verification);

        user.setActive(true);
        userRepository.save(user);

        auditLogService.log(user.getId(), user.getUsername(), "EMAIL_VERIFIED", "Email verified successfully");
        return createTokenResponse(user, "Unknown Device");
    }

    @Transactional
    public MessageResponse resendOtp(ResendOtpRequest request) {
        String email = SanitizeUtil.sanitize(request.getEmail()).toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Email not found"));

        if (user.isActive()) {
            throw new BadRequestException("Account is already verified");
        }

        sendVerificationCode(user, PURPOSE_REGISTRATION);
        return new MessageResponse("OTP resent successfully");
    }

    @Transactional
    public TokenResponse login(LoginRequest request, String deviceInfo) {
        String username = SanitizeUtil.sanitize(request.getUsername());

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword()));
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            User user = principal.getUser();

            if (!user.isActive()) {
                throw new UnauthorizedException("Account is not activated. Please verify your email.");
            }

            auditLogService.log(user.getId(), username, "USER_LOGIN", "User logged in");
            return createTokenResponse(user, deviceInfo);
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid username or password");
        }
    }

    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request, String deviceInfo) {
        Session session = sessionRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (session.getExpiresAt().isBefore(Instant.now())) {
            sessionRepository.delete(session);
            throw new UnauthorizedException("Refresh token has expired");
        }

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is not active");
        }

        sessionRepository.delete(session);
        return createTokenResponse(user, deviceInfo != null ? deviceInfo : session.getDeviceInfo());
    }

    @Transactional
    public MessageResponse logout(UserPrincipal principal, String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            sessionRepository.deleteByRefreshToken(refreshToken);
        }
        if (principal != null) {
            auditLogService.log(principal.getId(), principal.getUsername(), "USER_LOGOUT", "User logged out");
        }
        return new MessageResponse("Logged out successfully");
    }

    private TokenResponse createTokenResponse(User user, String deviceInfo) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtService.generateRefreshToken();

        String info = SanitizeUtil.sanitize(deviceInfo);
        if (info != null && info.length() > 500) {
            info = info.substring(0, 500);
        }

        Session session = new Session();
        session.setUserId(user.getId());
        session.setRefreshToken(refreshToken);
        session.setDeviceInfo(info);
        session.setExpiresAt(Instant.now().plus(jwtService.getRefreshTokenExpirationMs(), ChronoUnit.MILLIS));
        sessionRepository.save(session);

        return new TokenResponse(accessToken, refreshToken, jwtService.getAccessTokenExpirationMs() / 1000);
    }

    private void sendVerificationCode(User user, String purpose) {
        emailVerificationRepository.invalidateUnused(user.getId(), purpose);

        String code = OtpGenerator.generate();
        EmailVerification verification = new EmailVerification();
        verification.setUserId(user.getId());
        verification.setCode(code);
        verification.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        verification.setPurpose(purpose);
        emailVerificationRepository.save(verification);

        String purposeLabel = PURPOSE_REGISTRATION.equals(purpose) ? "registration" : "password change";
        emailService.sendOtp(user.getEmail(), code, purposeLabel);
    }
}
