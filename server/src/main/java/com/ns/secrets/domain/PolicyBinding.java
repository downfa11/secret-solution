package com.ns.secrets.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyBinding {
    private String memberId;
    private MemberType memberType; // USER, GROUP
    private List<String> attachedPolicies;

    public enum MemberType {
        USER,
        GROUP
    }
}
