package com.ns.secrets.command.service;


import com.ns.secrets.command.domain.Policy;

import java.util.List;
import java.util.Optional;

public interface PolicyService {
    List<Policy> getAllPolicies();
    Optional<Policy> getPolicyById(String id);
}

