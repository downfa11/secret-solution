package com.ns.secrets.shell;

import com.ns.secrets.domain.Policy;
import com.ns.secrets.domain.PolicyBinding;
import com.ns.secrets.service.PolicyBindingService;
import com.ns.secrets.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ShellComponent
@RequiredArgsConstructor
public class PolicyCommands {
    private final PolicyService policyService;
    private final PolicyBindingService policyBindingService;

    @ShellMethod(key = "list-policies", value = "List all policies")
    public String listPolicies() {
        List<Policy> policies = policyService.getAllPolicies();
        if (policies.isEmpty()) return "No policies found.";
        return policies.stream()
                .map(Policy::toString)
                .collect(Collectors.joining("\n"));
    }

    @ShellMethod(key = "get-policy", value = "Get a policy by policyId")
    public String getPolicyById(String id) {
        Optional<Policy> policyOpt = policyService.getPolicyById(id);
        if (policyOpt.isEmpty()) {
            return "No policy found with ID: " + id;
        }
        Policy policy = policyOpt.get();
        return policy.toString();
    }


    @ShellMethod(key = "bind-policies", value = "Bind policies to a user or group")
    public String bindPoliciesToMember(String memberId, String memberTypeStr, List<String> policyIds) {
        PolicyBinding.MemberType memberType;
        try {
            memberType = PolicyBinding.MemberType.valueOf(memberTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Invalid memberType: " + memberTypeStr + ". Allowed values: USER, GROUP";
        }
        policyBindingService.bindPoliciesToMember(memberId, memberType, policyIds);
        return String.format("Policies %s bound to %s %s",
                String.join(", ", policyIds), memberType.name().toLowerCase(), memberId);
    }

    @ShellMethod(key = "unbind-policies", value = "Unbind policies from a user or group")
    public String unbindPoliciesFromMember(String memberId, String memberTypeStr) {
        PolicyBinding.MemberType memberType;
        try {
            memberType = PolicyBinding.MemberType.valueOf(memberTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Invalid memberType: " + memberTypeStr + ". Allowed values: USER, GROUP";
        }
        policyBindingService.unbindPoliciesFromMember(memberId, memberType);
        return String.format("Policies unbound from %s %s", memberType.name().toLowerCase(), memberId);
    }

    @ShellMethod(key = "unbind-policy-from-members", value = "Unbind a policy from all members")
    public String unbindPolicyFromAllMembers(String policyId) {
        try {
            policyBindingService.unbindPolicyFromAllMembers(policyId);
            return "Unbound policy " + policyId + " from all members.";
        } catch (Exception e) {
            return "Failed to unbind policy from members: " + e.getMessage();
        }
    }


    @ShellMethod(key = "get-policy-binding", value = "Get policy binding for a user or group")
    public String getPolicyBindingForMember(String memberId, String memberTypeStr) {
        PolicyBinding.MemberType memberType;
        try {
            memberType = PolicyBinding.MemberType.valueOf(memberTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Invalid memberType: " + memberTypeStr + ". Allowed values: USER, GROUP";
        }
        var binding = policyBindingService.getPolicyBindingForMember(memberId, memberType);
        if (binding == null) {
            return String.format("No policy binding found for %s %s", memberType.name().toLowerCase(), memberId);
        }
        return String.format("Policy binding for %s %s: %s", memberType.name().toLowerCase(), memberId, binding);
    }
}