package com.ns.secrets.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.secrets.repository.EtcdRepository;
import com.ns.secrets.domain.PolicyBinding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyBindingService {
    private final EtcdRepository etcdRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void bindPoliciesToMember(String memberId, PolicyBinding.MemberType memberType, List<String> policyIds) {
        String key = buildKey(memberType, memberId);
        PolicyBinding existingBinding = getPolicyBindingForMember(memberId, memberType);

        List<String> mergedPolicies;
        if (existingBinding != null && existingBinding.getAttachedPolicies() != null) {
            mergedPolicies = Stream.concat(existingBinding.getAttachedPolicies().stream(), policyIds.stream())
                    .distinct()
                    .collect(Collectors.toList());
        } else {
            mergedPolicies = policyIds;
        }

        PolicyBinding policyBinding = PolicyBinding.builder()
                .memberId(memberId)
                .memberType(memberType)
                .attachedPolicies(mergedPolicies)
                .build();

        etcdRepository.put(key, serialize(policyBinding));
        log.info("Binding Policy {}: {} → {}", memberType, memberId, mergedPolicies);
    }


    public void unbindPoliciesFromMember(String memberId, PolicyBinding.MemberType memberType) {
        String key = buildKey(memberType, memberId);
        etcdRepository.delete(key);
        log.info("UnBinding Policy {}: {}", memberType, memberId);
    }

    public PolicyBinding getPolicyBindingForMember(String memberId, PolicyBinding.MemberType memberType) {
        String key = buildKey(memberType, memberId);
        String data = etcdRepository.get(key);

        if (data == null || data.isBlank()) {
            log.warn("Not found {}: {}", memberType, memberId);
            return null;
        }

        PolicyBinding binding = deserialize(data);
        log.info("getPolicyBindingForMember {}: {} → {}", memberType, memberId, binding.getAttachedPolicies());
        return binding;
    }

    public void unbindPolicyFromAllMembers(String policyId) {
        String prefix = "/policy-bindings/";
        List<String> keys = etcdRepository.getAllKeys(prefix);

        for (String key : keys) {
            String data = etcdRepository.get(key);
            if (data == null || data.isBlank()) continue;

            PolicyBinding binding = deserialize(data);
            if (binding.getAttachedPolicies() == null) continue;

            List<String> updatedPolicies = binding.getAttachedPolicies().stream()
                    .filter(pid -> !pid.equals(policyId))
                    .collect(Collectors.toList());

            if (updatedPolicies.size() == binding.getAttachedPolicies().size()) {
                continue;
            }

            if (updatedPolicies.isEmpty()) {
                etcdRepository.delete(key);
                log.info("Empty policy binding {}: {}", binding.getMemberType(), binding.getMemberId());
            } else {
                PolicyBinding updated = PolicyBinding.builder()
                        .memberId(binding.getMemberId())
                        .memberType(binding.getMemberType())
                        .attachedPolicies(updatedPolicies)
                        .build();
                etcdRepository.put(key, serialize(updated));
                log.info("Unbound policy {}: {}", binding.getMemberType(), binding.getMemberId());
            }
        }
    }

    private String buildKey(PolicyBinding.MemberType memberType, String memberId) {
        return "/policy-bindings/" + memberType.name().toLowerCase() + "/" + memberId;
    }

    private String serialize(PolicyBinding policyBinding) {
        try {
            return objectMapper.writeValueAsString(policyBinding);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("serialize error ", e);
        }
    }

    private PolicyBinding deserialize(String data) {
        try {
            return objectMapper.readValue(data, PolicyBinding.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("deserialize error ", e);
        }
    }
}
