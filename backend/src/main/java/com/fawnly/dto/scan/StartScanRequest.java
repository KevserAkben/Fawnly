package com.fawnly.dto.scan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class StartScanRequest {

    @NotBlank(message = "Project name is required")
    @Size(max = 100, message = "Project name must be at most 100 characters")
    private String projectName;

    @NotBlank(message = "Source type is required")
    @Pattern(regexp = "^(git|zip)$", message = "Source type must be 'git' or 'zip'")
    private String sourceType;

    private String githubUrl;

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }
}
