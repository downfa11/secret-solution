package com.ns.secrets.service;

import com.ns.secrets.domain.Policy;
import com.ns.secrets.domain.PolicyBinding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {
    private final PolicyService policyService;
    private final PolicyBindingService policyBindingService;
    private final UserService userService;


    public boolean isAllowed(String userId, String action, String resource) {
        PolicyBinding userBinding = policyBindingService.getPolicyBindingForMember(userId, PolicyBinding.MemberType.USER);
        String group = userService.getGroupForUser(userId);
        PolicyBinding groupBinding = group != null ? policyBindingService.getPolicyBindingForMember(group, PolicyBinding.MemberType.GROUP) : null;

        List<Policy> policies = new ArrayList<>();
        if (userBinding != null && userBinding.getAttachedPolicies() != null) {
            List<Policy> userPolicies = policyService.getPoliciesByIds(userBinding.getAttachedPolicies());
            policies.addAll(userPolicies);
        }
        if (groupBinding != null && groupBinding.getAttachedPolicies() != null) {
            List<Policy> groupPolicies = policyService.getPoliciesByIds(groupBinding.getAttachedPolicies());
            policies.addAll(groupPolicies);
        }

        for (Policy policy : policies) {
            for (Policy.Statement stmt : policy.getStatement()) {
                log.info("Policy: {}", policy.getId(), " - Statement: effect={}, actions={}, resources={}", stmt.getEffect(), stmt.getActions(), stmt.getResources());

                if (!"allow".equalsIgnoreCase(stmt.getEffect())) continue;

                boolean actionMatch = stmt.getActions().contains(action);
                boolean resourceMatch = stmt.getResources().stream().anyMatch(r -> match(resource, r));

                log.info("   -> Action match: {}, Resource match: {}", actionMatch, resourceMatch);

                if (actionMatch && resourceMatch) {
                    log.info("Permission granted policy {}", policy.getId());
                    return true;
                }
            }
        }

        log.info("Permission denied userId={} on action={}, resource={}", userId, action, resource);
        return false;
    }

    private boolean match(String actual, String pattern) {
        if (pattern.endsWith("/*")) {
            boolean matched = actual.startsWith(pattern.substring(0, pattern.length() - 1));
            return matched;
        }
        return actual.equals(pattern);
    }
}
