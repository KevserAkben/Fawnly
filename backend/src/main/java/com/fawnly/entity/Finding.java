package com.fawnly.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "findings")
public class Finding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scan_id", nullable = false)
    private Long scanId;

    @Column(name = "rule_id", nullable = false, length = 100)
    private String ruleId;

    @Column(nullable = false, length = 10)
    private String severity;

    @Column(name = "file_path", nullable = false, columnDefinition = "TEXT")
    private String filePath;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "owasp_code", length = 20)
    private String owaspCode;

    @Column(length = 20)
    private String cwe;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "triage_status", nullable = false, length = 30)
    private String triageStatus = "Needs Review";

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScanId() { return scanId; }
    public void setScanId(Long scanId) { this.scanId = scanId; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Integer getLineNo() { return lineNo; }
    public void setLineNo(Integer lineNo) { this.lineNo = lineNo; }
    public String getOwaspCode() { return owaspCode; }
    public void setOwaspCode(String owaspCode) { this.owaspCode = owaspCode; }
    public String getCwe() { return cwe; }
    public void setCwe(String cwe) { this.cwe = cwe; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTriageStatus() { return triageStatus; }
    public void setTriageStatus(String triageStatus) { this.triageStatus = triageStatus; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
