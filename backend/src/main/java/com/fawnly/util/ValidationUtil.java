package com.fawnly.util;

import java.util.regex.Pattern;

public final class ValidationUtil {

    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
            "^https?://(www\\.)?github\\.com/[a-zA-Z0-9_.-]+/[a-zA-Z0-9_.-]+(/.*)?$");

    private ValidationUtil() {}

    public static boolean isValidGithubUrl(String url) {
        return url != null && GITHUB_URL_PATTERN.matcher(url.trim()).matches();
    }

    public static boolean isValidZipFilename(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".zip");
    }
}
