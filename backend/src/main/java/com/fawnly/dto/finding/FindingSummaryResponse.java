package com.fawnly.dto.finding;

import java.time.Instant;
import java.util.List;

public class FindingSummaryResponse {

    private Long scanId;
    private String projectName;
    private Instant scanDate;
    private long total;
    private long high;
    private long medium;
    private long low;
    private List<FindingResponse> findings;

    public Long getScanId() { return scanId; }
    public void setScanId(Long scanId) { this.scanId = scanId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public Instant getScanDate() { return scanDate; }
    public void setScanDate(Instant scanDate) { this.scanDate = scanDate; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public long getHigh() { return high; }
    public void setHigh(long high) { this.high = high; }
    public long getMedium() { return medium; }
    public void setMedium(long medium) { this.medium = medium; }
    public long getLow() { return low; }
    public void setLow(long low) { this.low = low; }
    public List<FindingResponse> getFindings() { return findings; }
    public void setFindings(List<FindingResponse> findings) { this.findings = findings; }
}
