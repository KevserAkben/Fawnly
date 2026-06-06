package com.fawnly.repository;

import com.fawnly.entity.Scan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ScanRepository extends JpaRepository<Scan, Long> {
    List<Scan> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Scan> findByIdAndUserId(Long id, Long userId);
}
