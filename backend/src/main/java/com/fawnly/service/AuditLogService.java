package com.fawnly.service;

import com.fawnly.entity.AuditLog;
import com.fawnly.repository.AuditLogRepository;
import com.fawnly.util.SanitizeUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(Long userId, String username, String action, String details) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setUsername(SanitizeUtil.sanitizeForLog(username));
        log.setAction(SanitizeUtil.sanitizeForLog(action));
        log.setDetails(SanitizeUtil.sanitizeForLog(details));
        log.setIpAddress(resolveIpAddress());
        auditLogRepository.save(log);
    }

    private String resolveIpAddress() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }
}
