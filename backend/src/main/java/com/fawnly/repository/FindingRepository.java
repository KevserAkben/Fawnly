package com.fawnly.repository;

import com.fawnly.entity.Finding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FindingRepository extends JpaRepository<Finding, Long> {

    @Query("SELECT f FROM Finding f WHERE f.scanId = :scanId ORDER BY " +
            "CASE f.severity WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END, f.lineNo")
    List<Finding> findByScanIdOrderBySeverityAscLineNoAsc(@Param("scanId") Long scanId);

    Optional<Finding> findByIdAndScanId(Long id, Long scanId);

    long countByScanId(Long scanId);

    void deleteByScanId(Long scanId);
}
