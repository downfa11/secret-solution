package com.ns.secrets.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.ns.secrets.domain.Policy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyService {

    @Value("${git.repo.local-path}")
    private String localRepoPath;

    @Value("${policies.location:policies}")
    private String policiesLocation;

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    public Optional<Policy> getPolicyById(String id) {
        Path policyPath = Paths.get(localRepoPath, policiesLocation, id + ".yaml");
        if (!Files.exists(policyPath)) return Optional.empty();

        try (InputStream in = Files.newInputStream(policyPath)) {
            Policy policy = yamlMapper.readValue(in, Policy.class);
            return Optional.of(policy);
        } catch (Exception e) {
            log.error("getPolicyById error: {}", id, e);
            return Optional.empty();
        }
    }


    public List<Policy> getAllPolicies() {
        try {
            Path policiesDir = Paths.get(localRepoPath, policiesLocation);
            try (Stream<Path> files = Files.list(policiesDir)) {
                return files.filter(path -> path.toString().endsWith(".yaml"))
                        .map(path -> {
                            try (InputStream in = Files.newInputStream(path)) {
                                return yamlMapper.readValue(in, Policy.class);
                            } catch (Exception e) {
                                log.error("File input error: {}", path, e);
                                return null;
                            }
                        })
                        .filter(p -> p != null)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            throw new RuntimeException("getAllPolicies error", e);
        }
    }

    public List<Policy> getPoliciesByIds(List<String> policyIds) {
        return policyIds.stream()
                .map(this::getPolicyById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}
