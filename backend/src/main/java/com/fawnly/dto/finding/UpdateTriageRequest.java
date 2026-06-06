package com.fawnly.dto.finding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateTriageRequest {

    @NotBlank(message = "Triage status is required")
    @Pattern(regexp = "^(True Positive|False Positive|Not Exploitable|Needs Review)$",
            message = "Invalid triage status")
    private String triageStatus;

    public String getTriageStatus() { return triageStatus; }
    public void setTriageStatus(String triageStatus) { this.triageStatus = triageStatus; }
}
