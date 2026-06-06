package com.fawnly.repository;

import com.fawnly.entity.Finding;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FindingRepository extends JpaRepository<Finding, Long> {
    List<Finding> findByScanIdOrderBySeverityAscLineNoAsc(Long scanId);
    Optional<Finding> findByIdAndScanId(Long id, Long scanId);
    long countByScanId(Long scanId);
    void deleteByScanId(Long scanId);
}
