package com.fawnly.dto.scan;

public class ScanStatusResponse {

    private Long id;
    private String status;
    private String errorMessage;
    private int progress;

    public ScanStatusResponse() {}

    public ScanStatusResponse(Long id, String status, String errorMessage) {
        this.id = id;
        this.status = status;
        this.errorMessage = errorMessage;
        this.progress = switch (status) {
            case "queued" -> 10;
            case "running" -> 50;
            case "done" -> 100;
            case "failed" -> 100;
            default -> 0;
        };
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
}
