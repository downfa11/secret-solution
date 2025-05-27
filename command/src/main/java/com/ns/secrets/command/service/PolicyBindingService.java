package com.ns.secrets.command.service;


import com.ns.secrets.command.domain.PolicyBinding;

import java.util.List;

public interface PolicyBindingService {
    void bindPoliciesToMember(String memberId, PolicyBinding.MemberType memberType, List<String> policyIds);
    void unbindPoliciesFromMember(String memberId, PolicyBinding.MemberType memberType);
    void unbindPolicyFromAllMembers(String policyId);
    PolicyBinding getPolicyBindingForMember(String memberId, PolicyBinding.MemberType memberType);
}

