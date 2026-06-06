package com.fawnly.dto.settings;

import java.time.Instant;

public class SessionResponse {

    private Long id;
    private String deviceInfo;
    private Instant createdAt;
    private boolean current;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public boolean isCurrent() { return current; }
    public void setCurrent(boolean current) { this.current = current; }
}
