package com.ns.secrets.command.service;


import com.ns.secrets.command.domain.PolicyBinding;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyBindingServiceImpl implements PolicyBindingService {

    private final WebClient webClient;

    @Override
    public void bindPoliciesToMember(String memberId, PolicyBinding.MemberType memberType, List<String> policyIds) {
        webClient.post()
                .uri("/api/policies/bind")
                .bodyValue(new BindRequest(memberId, memberType, policyIds))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public void unbindPoliciesFromMember(String memberId, PolicyBinding.MemberType memberType) {
        webClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/policies/bind")
                        .queryParam("memberId", memberId)
                        .queryParam("memberType", memberType.name())
                        .build())
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public void unbindPolicyFromAllMembers(String policyId) {
        webClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/policies/{policyId}/unbind-all")
                        .build(policyId))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public PolicyBinding getPolicyBindingForMember(String memberId, PolicyBinding.MemberType memberType) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/policies/binding")
                        .queryParam("memberId", memberId)
                        .queryParam("memberType", memberType.name())
                        .build())
                .retrieve()
                .bodyToMono(PolicyBinding.class)
                .block();
    }

    private record BindRequest(String memberId, PolicyBinding.MemberType memberType, List<String> policyIds) {}
}
