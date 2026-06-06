package com.fawnly.dto.finding;

import java.time.Instant;

public class FindingResponse {

    private Long id;
    private Long scanId;
    private String ruleId;
    private String severity;
    private String filePath;
    private Integer lineNo;
    private String owaspCode;
    private String cwe;
    private String message;
    private String triageStatus;
    private String note;
    private Instant updatedAt;

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
