package com.fawnly.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fawnly.entity.Finding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SemgrepService {

    private static final Logger log = LoggerFactory.getLogger(SemgrepService.class);
    private static final int SCAN_TIMEOUT_MINUTES = 10;

    private final ObjectMapper objectMapper;

    @Value("${fawnly.scan.semgrep-path:semgrep}")
    private String semgrepPath;

    @Value("${fawnly.scan.rules-path:rules/owasp_java.yaml}")
    private String rulesPath;

    public SemgrepService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<Finding> runScan(Long scanId, Path sourceDir) throws Exception {
        Path resolvedRules = resolveRulesPath();
        Path resultFile = sourceDir.getParent().resolve("semgrep-result.json");
        Path errorFile = sourceDir.getParent().resolve("semgrep-error.log");

        ProcessBuilder pb = new ProcessBuilder(
                semgrepPath,
                "--config", resolvedRules.toString(),
                "--json",
                "--quiet",
                "--metrics=off",
                "--timeout", "30",
                sourceDir.toString()
        );
        pb.redirectOutput(resultFile.toFile());
        pb.redirectError(errorFile.toFile());

        Process process = pb.start();
        boolean finished = process.waitFor(SCAN_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Semgrep timed out");
        }

        int exitCode = process.exitValue();
        // 0 = no findings, 1 = findings found; anything else is a tool error
        if (exitCode != 0 && exitCode != 1) {
            String stderr = Files.exists(errorFile) ? Files.readString(errorFile) : "";
            log.warn("Semgrep exited with code {} for scan {}: {}", scanId, exitCode, stderr);
            throw new RuntimeException("Semgrep failed with exit code " + exitCode);
        }

        if (!Files.exists(resultFile) || Files.size(resultFile) == 0) {
            return List.of();
        }

        return parseResults(scanId, resultFile, sourceDir);
    }

    private Path resolveRulesPath() {
        Path configured = Paths.get(rulesPath);
        if (Files.isRegularFile(configured)) {
            return configured.toAbsolutePath().normalize();
        }

        Path fromCwdParent = Paths.get("..").resolve(rulesPath);
        if (Files.isRegularFile(fromCwdParent)) {
            return fromCwdParent.toAbsolutePath().normalize();
        }

        throw new IllegalStateException("Semgrep rules file not found: " + rulesPath);
    }

    private List<Finding> parseResults(Long scanId, Path resultFile, Path sourceDir) throws Exception {
        JsonNode root = objectMapper.readTree(resultFile.toFile());
        JsonNode results = root.get("results");

        List<Finding> findings = new ArrayList<>();
        if (results == null || !results.isArray()) {
            return findings;
        }

        String basePath = sourceDir.toAbsolutePath().toString();

        for (JsonNode result : results) {
            Finding finding = new Finding();
            finding.setScanId(scanId);

            JsonNode checkId = result.get("check_id");
            finding.setRuleId(truncate(checkId != null ? checkId.asText() : "unknown", 255));

            JsonNode extra = result.get("extra");
            if (extra != null) {
                JsonNode severity = extra.get("severity");
                finding.setSeverity(mapSeverity(severity != null ? severity.asText() : "MEDIUM"));

                JsonNode message = extra.get("message");
                finding.setMessage(message != null ? message.asText() : "Security finding detected");

                JsonNode metadata = extra.get("metadata");
                if (metadata != null) {
                    finding.setOwaspCode(firstMetadataText(metadata.get("owasp"), 50));
                    finding.setCwe(firstMetadataText(metadata.get("cwe"), 50));
                }
            } else {
                finding.setSeverity("MEDIUM");
                finding.setMessage("Security finding detected");
            }

            JsonNode path = result.get("path");
            String filePath = path != null ? path.asText() : "unknown";
            if (filePath.startsWith(basePath)) {
                filePath = filePath.substring(basePath.length());
                if (filePath.startsWith("/") || filePath.startsWith("\\")) {
                    filePath = filePath.substring(1);
                }
            }
            finding.setFilePath(filePath.isBlank() ? "unknown" : filePath);

            JsonNode start = result.get("start");
            if (start != null && start.has("line")) {
                finding.setLineNo(start.get("line").asInt());
            } else {
                finding.setLineNo(1);
            }

            finding.setTriageStatus("Needs Review");
            findings.add(finding);
        }

        return findings;
    }

    private String firstMetadataText(JsonNode node, int maxLen) {
        if (node == null || node.isNull()) {
            return null;
        }
        String value;
        if (node.isArray()) {
            if (node.isEmpty()) {
                return null;
            }
            value = node.get(0).asText();
        } else {
            value = node.asText();
        }
        return truncate(value, maxLen);
    }

    private String mapSeverity(String semgrepSeverity) {
        if (semgrepSeverity == null) {
            return "MEDIUM";
        }
        return switch (semgrepSeverity.toUpperCase()) {
            case "ERROR", "HIGH" -> "HIGH";
            case "WARNING", "MEDIUM" -> "MEDIUM";
            default -> "LOW";
        };
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
