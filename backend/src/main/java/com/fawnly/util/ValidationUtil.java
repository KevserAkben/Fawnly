package com.fawnly.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ValidationUtil {

    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
            "^https://(?:www\\.)?github\\.com/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)(?:/.*)?/?$",
            Pattern.CASE_INSENSITIVE);

    private ValidationUtil() {}

    public static boolean isValidGithubUrl(String url) {
        return normalizeGithubUrl(url) != null;
    }

    /**
     * Accepts common GitHub paste formats (with .git, trailing slash, or /tree/...)
     * and returns a canonical https://github.com/owner/repo.git URL.
     */
    public static String normalizeGithubUrl(String url) {
        if (url == null) {
            return null;
        }
        Matcher matcher = GITHUB_URL_PATTERN.matcher(url.trim());
        if (!matcher.matches()) {
            return null;
        }
        String owner = matcher.group(1);
        String repo = matcher.group(2);
        if (repo.toLowerCase().endsWith(".git")) {
            repo = repo.substring(0, repo.length() - 4);
        }
        if (".".equals(owner) || "..".equals(owner) || repo.isBlank() || ".".equals(repo) || "..".equals(repo)) {
            return null;
        }
        return "https://github.com/" + owner + "/" + repo + ".git";
    }

    public static boolean isValidZipFilename(String filename) {
        if (filename == null) {
            return false;
        }
        String name = filename.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        return name.toLowerCase().endsWith(".zip") && name.length() > 4;
    }
}
