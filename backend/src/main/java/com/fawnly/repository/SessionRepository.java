package com.fawnly.repository;

import com.fawnly.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {
    Optional<Session> findByRefreshToken(String refreshToken);
    List<Session> findByUserIdOrderByCreatedAtDesc(Long userId);
    void deleteByRefreshToken(String refreshToken);
    void deleteByUserId(Long userId);
    void deleteByUserIdAndIdNot(Long userId, Long sessionId);
}
