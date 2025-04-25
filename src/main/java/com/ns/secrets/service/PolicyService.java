package com.ns.secrets.service;

import com.ns.secrets.domain.Policy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PolicyService {

    @Value("${policies.location:}")
    private String policiesLocation;

    private final PolicyBindingService policyBindingService;
    private final GitService gitService;

    public void syncPoliciesFromGit() {
        try {
            gitService.syncPolicyFile();

            Path policiesDir = Paths.get("/path/to/local/repo", policiesLocation);
            try (Stream<Path> files = Files.list(policiesDir)) {
                List<Path> yamlFiles = files.filter(path -> path.toString().endsWith(".yaml"))
                        .collect(Collectors.toList());

                for (Path file : yamlFiles) {
                    try (InputStream in = Files.newInputStream(file)) {
                        Yaml yaml = new Yaml();
                        Policy policy = yaml.loadAs(in, Policy.class);
                        applyPolicy(policy);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyPolicy(Policy policy) {
        System.out.println("Applying policy: " + policy);

        List<String> userGroups = policy.getStatement().stream()
                .flatMap(statement -> statement.getResources().stream())
                .collect(Collectors.toList());

        for (String group : userGroups) {
            List<String> policies = List.of(policy.getId());
            policyBindingService.bindPoliciesToGroup(group, policies);
            System.out.println("Policy " + policy.getId() + " has been bound to group: " + group);
        }
    }
}
