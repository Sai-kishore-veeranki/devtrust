package com.vsk.devtrust.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class GitHubCommentService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${devtrust.github.token}")
    private String githubToken;

    public void postRiskComment(String repoFullName, int prNumber,
                                RiskScorerService.RiskScore riskScore) {
        try {
            String url = "https://api.github.com/repos/" + repoFullName + "/issues/" + prNumber + "/comments";

            String emoji = riskScore.score() >= 70 ? "🔴" : riskScore.score() >= 40 ? "🟡" : "🟢";
            String body = """
                    ## %s DevTrust Risk Score: %d/100 — %s
                    
                    %s
                    
                    | Signal | Detail |
                    |--------|--------|
                    | Risk level | %s |
                    | Score | %d / 100 |
                    
                    > Powered by [DevTrust](https://github.com/Sai-kishore-veeranki/devtrust) — automated deployment risk analysis
                    """.formatted(
                    emoji,
                    riskScore.score(),
                    riskScore.level(),
                    riskScore.explanation(),
                    riskScore.level(),
                    riskScore.score()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + githubToken);
            headers.set("X-GitHub-Api-Version", "2022-11-28");

            HttpEntity<Map<String, String>> request = new HttpEntity<>(
                    Map.of("body", body), headers);

            restTemplate.postForObject(url, request, String.class);

            log.info("Posted risk score comment on PR #{} in {}", prNumber, repoFullName);

        } catch (Exception e) {
            log.error("Failed to post risk comment on PR #{} in {}: {}",
                    prNumber, repoFullName, e.getMessage());
        }
    }
}