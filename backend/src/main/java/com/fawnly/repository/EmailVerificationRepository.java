package com.fawnly.repository;

import com.fawnly.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findTopByUserIdAndPurposeAndUsedFalseOrderByExpiresAtDesc(
            Long userId, String purpose);
    Optional<EmailVerification> findByUserIdAndCodeAndPurposeAndUsedFalse(
            Long userId, String code, String purpose);
}
