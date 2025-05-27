package com.ns.secrets.command.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GitServiceImpl implements GitService {
    private final WebClient webClient;
    
    @Override
    public String sync() {
        try {
            return webClient.post()
                    .uri("/api/git/sync")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            return "Failed to sync Git repository: " + e.getResponseBodyAsString();
        } catch (Exception e) {
            return "Failed to sync Git repository: " + e.getMessage();
        }
    }

    @Override
    public String rollback(String commitHash) {
        try {
            return webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/git/rollback")
                            .queryParam("commitHash", commitHash)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            return "Rollback failed: " + e.getResponseBodyAsString();
        } catch (Exception e) {
            return "Rollback failed: " + e.getMessage();
        }
    }

    @Override
    public String branch(String branchName) {
        try {
            return webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/git/branch")
                            .queryParam("branchName", branchName)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            return "Branch checkout failed: " + e.getResponseBodyAsString();
        } catch (Exception e) {
            return "Branch checkout failed: " + e.getMessage();
        }
    }

    @Override
    public String currentCommit() {
        try {
            return webClient.get()
                    .uri("/api/git/current-commit")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            return "Failed to get current commit: " + e.getResponseBodyAsString();
        } catch (Exception e) {
            return "Failed to get current commit: " + e.getMessage();
        }
    }

    @Override
    public List<String> recentCommits(int count) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/git/recent-commits")
                            .queryParam("count", count)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            return Collections.singletonList("Failed to get commits: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            return Collections.singletonList("Failed to get commits: " + e.getMessage());
        }
    }
}

