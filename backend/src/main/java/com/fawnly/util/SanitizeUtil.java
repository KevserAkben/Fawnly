package com.fawnly.util;

import org.owasp.encoder.Encode;

public final class SanitizeUtil {

    private SanitizeUtil() {}

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return Encode.forHtml(input.trim());
    }

    public static String sanitizeForLog(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("[\\r\\n]", "").trim();
    }
}
