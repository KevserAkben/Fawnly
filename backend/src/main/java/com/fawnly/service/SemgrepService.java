package com.fawnly.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fawnly.entity.Finding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class SemgrepService {

    private static final Logger log = LoggerFactory.getLogger(SemgrepService.class);

    private final ObjectMapper objectMapper;

    @Value("${fawnly.scan.semgrep-path:semgrep}")
    private String semgrepPath;

    @Value("${fawnly.scan.rules-path:rules/owasp_java.yaml}")
    private String rulesPath;

    public SemgrepService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<Finding> runScan(Long scanId, Path sourceDir) throws Exception {
        Path resultFile = sourceDir.getParent().resolve("semgrep-result.json");

        ProcessBuilder pb = new ProcessBuilder(
                semgrepPath,
                "--config", rulesPath,
                "--json",
                "--quiet",
                sourceDir.toString()
        );
        pb.redirectErrorStream(true);
        pb.redirectOutput(resultFile.toFile());

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) {
                // drain output
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0 && exitCode != 1) {
            log.warn("Semgrep exited with code {} for scan {}", exitCode, scanId);
        }

        if (!Files.exists(resultFile)) {
            return List.of();
        }

        return parseResults(scanId, resultFile, sourceDir);
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
            finding.setRuleId(checkId != null ? checkId.asText() : "unknown");

            JsonNode extra = result.get("extra");
            if (extra != null) {
                JsonNode severity = extra.get("severity");
                finding.setSeverity(mapSeverity(severity != null ? severity.asText() : "MEDIUM"));

                JsonNode message = extra.get("message");
                finding.setMessage(message != null ? message.asText() : "Security finding detected");

                JsonNode metadata = extra.get("metadata");
                if (metadata != null) {
                    JsonNode owasp = metadata.get("owasp");
                    finding.setOwaspCode(owasp != null ? owasp.asText() : null);
                    JsonNode cwe = metadata.get("cwe");
                    finding.setCwe(cwe != null ? cwe.asText() : null);
                }
            } else {
                finding.setSeverity("MEDIUM");
                finding.setMessage("Security finding detected");
            }

            JsonNode path = result.get("path");
            String filePath = path != null ? path.asText() : "unknown";
            if (filePath.startsWith(basePath)) {
                filePath = filePath.substring(basePath.length());
                if (filePath.startsWith("/")) {
                    filePath = filePath.substring(1);
                }
            }
            finding.setFilePath(filePath);

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

    private String mapSeverity(String semgrepSeverity) {
        return switch (semgrepSeverity.toUpperCase()) {
            case "ERROR", "HIGH" -> "HIGH";
            case "WARNING", "MEDIUM" -> "MEDIUM";
            default -> "LOW";
        };
    }
}
