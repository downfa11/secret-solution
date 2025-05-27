package com.ns.secrets.command.service;


import com.ns.secrets.command.domain.Policy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {
    private final WebClient webClient;


    @Override
    public List<Policy> getAllPolicies() {
        try {
            return webClient.get()
                    .uri("/api/policies")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Policy>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            // 필요하면 로그 찍기
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<Policy> getPolicyById(String id) {
        try {
            Policy policy = webClient.get()
                    .uri("/api/policies/{id}", id)
                    .retrieve()
                    .bodyToMono(Policy.class)
                    .block();
            return Optional.ofNullable(policy);
        } catch (WebClientResponseException e) {
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
