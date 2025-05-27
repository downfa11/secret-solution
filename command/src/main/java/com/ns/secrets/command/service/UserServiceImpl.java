package com.ns.secrets.command.service;

import com.ns.secrets.command.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final WebClient webClient;

    @Override
    public User getUser(String userId) {
        return webClient.get()
                .uri("/api/users/{userId}", userId)
                .retrieve()
                .bodyToMono(User.class)
                .block();
    }

    @Override
    public List<String> getGroups() {
        return webClient.get()
                .uri("/api/users/groups")
                .retrieve()
                .bodyToFlux(String.class)
                .collectList()
                .block();
    }

    @Override
    public List<String> getMembers(String group) {
        return webClient.get()
                .uri("/api/users/groups/{group}/members", group)
                .retrieve()
                .bodyToFlux(String.class)
                .collectList()
                .block();
    }

    @Override
    public List<String> getUsersInGroup(String group) {
        return webClient.get()
                .uri("/api/users/groups/{group}/users", group)
                .retrieve()
                .bodyToFlux(String.class)
                .collectList()
                .block();
    }

    @Override
    public Map<String, List<String>> getAllGroupsWithMembers() {
        return webClient.get()
                .uri("/api/users/groups-with-members")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, List<String>>>() {})
                .block();
    }
}
