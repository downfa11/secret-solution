package com.ns.secrets.command.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;


@Service
@RequiredArgsConstructor
public class SecretServiceImpl implements SecretService {
    private final WebClient webClient;

    @Override
    public String setKey(String newKey) {
        return webClient.post()
                .uri("/api/secrets/set-key")
                .bodyValue(newKey)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    @Override
    public void saveRaw(String executorUserId, String namespace, String key, String plainText) {
        saveRaw(executorUserId, namespace, key, plainText, false, 0);
    }

    @Override
    public void saveRaw(String executorUserId, String namespace, String key, String plainText, boolean ttl, long ttlSeconds) {
        webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/secrets/put")
                        .queryParam("executorUserId", executorUserId)
                        .queryParam("namespace", namespace)
                        .queryParam("key", key)
                        .queryParam("plainText", plainText)
                        .build())
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    @Override
    public void saveEncrypted(String executorUserId, String namespace, String key, String plainText) {
        saveEncrypted(executorUserId, namespace, key, plainText, false, 0);
    }

    @Override
    public void saveEncrypted(String executorUserId, String namespace, String key, String plainText, boolean ttl, long ttlSeconds) {
        webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/secrets/encrypted")
                        .queryParam("executorUserId", executorUserId)
                        .queryParam("namespace", namespace)
                        .queryParam("key", key)
                        .queryParam("plainText", plainText)
                        .build())
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    @Override
    public String getDecrypted(String userId, String namespace, String key) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/secrets/decrypted")
                        .queryParam("userId", userId)
                        .queryParam("namespace", namespace)
                        .queryParam("key", key)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    @Override
    public String getRaw(String userId, String namespace, String key) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/secrets/raw")
                        .queryParam("userId", userId)
                        .queryParam("namespace", namespace)
                        .queryParam("key", key)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    @Override
    public List<String> getAllSecrets(String requesterUserId, String namespace) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/secrets/list")
                        .queryParam("requesterUserId", requesterUserId)
                        .queryParam("namespace", namespace)
                        .build())
                .retrieve()
                .bodyToFlux(String.class)
                .collectList()
                .block();
    }
}
