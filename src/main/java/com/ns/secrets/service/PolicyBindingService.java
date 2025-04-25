package com.ns.secrets.service;

import com.ns.secrets.domain.PolicyBinding;
import com.ns.secrets.repository.EtcdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyBindingService {
    private final EtcdRepository etcdRepository;

    public void bindPoliciesToGroup(String memberGroup, List<String> policyIds) {
        PolicyBinding policyBinding = PolicyBinding.builder()
                .memberGroup(memberGroup)
                .attachedPolicies(policyIds)
                .build();

        String key = "/policy-bindings/" + memberGroup;
        etcdRepository.put(key, serialize(policyBinding));
        System.out.println("Policy binding created for group: " + memberGroup);
    }

    public void unbindPoliciesFromGroup(String memberGroup) {
        String key = "/policy-bindings/" + memberGroup;
        etcdRepository.put(key, "");
        System.out.println("Policy binding removed for group: " + memberGroup);
    }

    public PolicyBinding getPolicyBindingForGroup(String memberGroup) {
        String key = "/policy-bindings/" + memberGroup;
        String data = etcdRepository.get(key);

        if (data != null) {
            return deserialize(data);
        } else {
            return null;
        }
    }

    private String serialize(PolicyBinding policyBinding) {
        return policyBinding.toString(); // todo JSON말고 그냥 집어넣음
    }

    private PolicyBinding deserialize(String data) {
        // todo 더미 데이터임
        return new PolicyBinding("default", List.of("policy1", "policy2"));
    }
}
