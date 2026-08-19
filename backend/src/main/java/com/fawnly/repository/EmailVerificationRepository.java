package com.fawnly.repository;

import com.fawnly.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findTopByUserIdAndPurposeAndUsedFalseOrderByExpiresAtDesc(
            Long userId, String purpose);
    Optional<EmailVerification> findByUserIdAndCodeAndPurposeAndUsedFalse(
            Long userId, String code, String purpose);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update EmailVerification e set e.used = true where e.userId = :userId and e.purpose = :purpose and e.used = false")
    void invalidateUnused(@Param("userId") Long userId, @Param("purpose") String purpose);
}
