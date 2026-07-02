package com.vsk.devtrust.service;

import com.vsk.devtrust.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskScorerService {

    private final IncidentRepository incidentRepository;

    private static final List<String> HIGH_RISK_KEYWORDS =
            List.of("refactor", "migrate", "remove", "breaking", "delete",
                    "rename", "restructure", "rewrite", "overhaul", "replace");

    private static final List<String> LOW_RISK_KEYWORDS =
            List.of("typo", "readme", "docs", "comment", "minor",
                    "lint", "format", "whitespace");

    public RiskScore compute(String serviceName, int changedFiles,
                             String prTitle, List<String> changedFilePaths) {

        int score = 0;
        StringBuilder explanation = new StringBuilder();

        // Signal 1 — recent incident history (0-40 points)
        int incidentScore = computeIncidentScore(serviceName, explanation);
        score += incidentScore;

        // Signal 2 — number of changed files (0-30 points)
        int fileScore = computeFileScore(changedFiles, explanation);
        score += fileScore;

        // Signal 3 — critical file paths touched (0-20 points)
        int pathScore = computePathScore(changedFilePaths, explanation);
        score += pathScore;

        // Signal 4 — PR title keywords (0-10 points)
        int keywordScore = computeKeywordScore(prTitle, explanation);
        score += keywordScore;

        score = Math.min(score, 100);

        String level = score >= 70 ? "HIGH" : score >= 40 ? "MEDIUM" : "LOW";

        log.info("Risk score for service [{}] PR '{}': {}/100 ({})",
                serviceName, prTitle, score, level);

        return new RiskScore(score, level, explanation.toString().trim());
    }

    private int computeIncidentScore(String serviceName, StringBuilder explanation) {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        long recentIncidents = incidentRepository
                .findByDetectedAtAfterOrderByDetectedAtDesc(thirtyDaysAgo)
                .stream()
                .filter(i -> serviceName.equals(i.getServiceName()))
                .count();

        if (recentIncidents == 0) {
            explanation.append("No recent incidents for this service. ");
            return 0;
        } else if (recentIncidents <= 2) {
            explanation.append(recentIncidents).append(" incident(s) in last 30 days (+20). ");
            return 20;
        } else if (recentIncidents <= 5) {
            explanation.append(recentIncidents).append(" incidents in last 30 days (+30). ");
            return 30;
        } else {
            explanation.append(recentIncidents).append(" incidents in last 30 days — high-risk service (+40). ");
            return 40;
        }
    }

    private int computeFileScore(int changedFiles, StringBuilder explanation) {
        if (changedFiles <= 3) {
            explanation.append("Small PR (").append(changedFiles).append(" files). ");
            return 5;
        } else if (changedFiles <= 10) {
            explanation.append("Medium PR (").append(changedFiles).append(" files, +15). ");
            return 15;
        } else if (changedFiles <= 25) {
            explanation.append("Large PR (").append(changedFiles).append(" files, +25). ");
            return 25;
        } else {
            explanation.append("Very large PR (").append(changedFiles).append(" files, +30). ");
            return 30;
        }
    }

    private int computePathScore(List<String> paths, StringBuilder explanation) {
        long criticalPaths = paths.stream()
                .filter(p -> p.contains("config") || p.contains("security") ||
                        p.contains("auth") || p.contains("payment") ||
                        p.contains("database") || p.contains("migration"))
                .count();

        if (criticalPaths == 0) return 0;

        explanation.append(criticalPaths)
                .append(" critical path(s) touched (config/security/auth, +")
                .append(Math.min(criticalPaths * 10, 20)).append("). ");
        return (int) Math.min(criticalPaths * 10, 20);
    }

    private int computeKeywordScore(String title, StringBuilder explanation) {
        String lower = title.toLowerCase();

        boolean hasHighRisk = HIGH_RISK_KEYWORDS.stream().anyMatch(lower::contains);
        boolean hasLowRisk = LOW_RISK_KEYWORDS.stream().anyMatch(lower::contains);

        if (hasHighRisk) {
            explanation.append("High-risk keyword in PR title (+10). ");
            return 10;
        }
        if (hasLowRisk) {
            explanation.append("Low-risk change based on PR title (-5). ");
            return -5;
        }
        return 0;
    }

    public record RiskScore(int score, String level, String explanation) {}
}