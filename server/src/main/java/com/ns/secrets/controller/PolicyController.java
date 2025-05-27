package com.ns.secrets.controller;


import com.ns.secrets.domain.Policy;
import com.ns.secrets.domain.PolicyBinding;
import com.ns.secrets.service.PolicyBindingService;
import com.ns.secrets.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;
    private final PolicyBindingService policyBindingService;

    private PolicyBinding.MemberType parseMemberType(String memberType) {
        try {
            return PolicyBinding.MemberType.valueOf(memberType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid memberType: " + memberType + ". Allowed values: USER, GROUP");
        }
    }

    @GetMapping
    public List<Policy> listPolicies() {
        return policyService.getAllPolicies();
    }

    @GetMapping("/{id}")
    public Policy getPolicyById(@PathVariable String id) {
        return policyService.getPolicyById(id)
                .orElseThrow(() -> new NoSuchElementException("No policy found with ID: " + id));
    }

    @PostMapping("/bind")
    public ResponseEntity<String> bindPoliciesToMember(
            @RequestParam String memberId,
            @RequestParam String memberType,
            @RequestBody List<String> policyIds) {

        PolicyBinding.MemberType type = parseMemberType(memberType);
        policyBindingService.bindPoliciesToMember(memberId, type, policyIds);
        return ResponseEntity.ok(String.format("Policies %s bound to %s %s",
                String.join(", ", policyIds), type.name().toLowerCase(), memberId));
    }

    @DeleteMapping("/bind")
    public ResponseEntity<String> unbindPoliciesFromMember(
            @RequestParam String memberId,
            @RequestParam String memberType) {

        PolicyBinding.MemberType type = parseMemberType(memberType);
        policyBindingService.unbindPoliciesFromMember(memberId, type);
        return ResponseEntity.ok(String.format("Policies unbound from %s %s", type.name().toLowerCase(), memberId));
    }

    @DeleteMapping("/{policyId}/unbind-all")
    public ResponseEntity<String> unbindPolicyFromAllMembers(@PathVariable String policyId) {
        policyBindingService.unbindPolicyFromAllMembers(policyId);
        return ResponseEntity.ok("Unbound policy " + policyId + " from all members.");
    }

    @GetMapping("/binding")
    public ResponseEntity<PolicyBinding> getPolicyBindingForMember(
            @RequestParam String memberId,
            @RequestParam String memberType) {

        PolicyBinding.MemberType type = parseMemberType(memberType);
        PolicyBinding binding = policyBindingService.getPolicyBindingForMember(memberId, type);
        if (binding == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(binding);
    }
}
