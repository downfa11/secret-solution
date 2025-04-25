package com.ns.secrets.shell;

import com.ns.secrets.service.PolicyBindingService;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.util.List;

@ShellComponent
@RequiredArgsConstructor
public class PolicyBindingCommands {

    private final PolicyBindingService policyBindingService;

    @ShellMethod(key = "bind-policies-to-group", value = "Binds policies to a group")
    public String bindPoliciesToGroup(String memberGroup, List<String> policyIds) {
        policyBindingService.bindPoliciesToGroup(memberGroup, policyIds);
        return "Policies " + String.join(", ", policyIds) + " bound to group " + memberGroup;
    }

    @ShellMethod(key = "unbind-policies-from-group", value = "Unbinds policies from a group")
    public String unbindPoliciesFromGroup(String memberGroup) {
        policyBindingService.unbindPoliciesFromGroup(memberGroup);
        return "Policies unbound from group " + memberGroup;
    }

    @ShellMethod(key = "get-policy-binding", value = "Gets the policy binding for a group")
    public String getPolicyBinding(String memberGroup) {
        var policyBinding = policyBindingService.getPolicyBindingForGroup(memberGroup);
        if (policyBinding == null) {
            return "No policy binding found for group " + memberGroup;
        }
        return "Policy binding for group " + memberGroup + ": " + policyBinding.toString();
    }
}
