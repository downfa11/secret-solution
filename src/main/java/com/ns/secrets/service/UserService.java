package com.ns.secrets.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.ns.secrets.domain.User;
import com.ns.secrets.domain.UserGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {


    @Value("${git.repo.local-path}")
    private String localRepoPath;

    @Value("${users.directory:users}")
    private String userDir;

    @Value("${user-groups.location:user-groups}")
    private String userGroupDir;

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private Map<String, User> loadUsers() {
        Path usersPath = Paths.get(localRepoPath, userDir);
        if (!Files.exists(usersPath) || !Files.isDirectory(usersPath)) {
            throw new RuntimeException("User not found: " + usersPath);
        }

        try (Stream<Path> stream = Files.list(usersPath)) {
            return stream.filter(p -> p.toString().endsWith(".yaml"))
                    .map(p -> {
                        try (InputStream in = Files.newInputStream(p)) {
                            return mapper.readValue(in, User.class);
                        } catch (Exception e) {
                            log.error("File input error: {}", p, e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(u -> u.getId() != null)
                    .collect(Collectors.toMap(User::getId, u -> u));
        } catch (Exception e) {
            throw new RuntimeException("loadUsers error ", e);
        }
    }

    public User getUser(String id) {
        return Optional.ofNullable(loadUsers().get(id))
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    public String getGroupForUser(String userId) {
        return Optional.ofNullable(loadUsers().get(userId))
                .map(User::getGroup)
                .orElse(null);
    }


    public List<String> getUsersInGroup(String group) {
        return loadUsers().values().stream()
                .filter(user -> group.equals(user.getGroup()))
                .map(User::getId)
                .collect(Collectors.toList());
    }

    public List<String> getGroups() {
        Path dir = Paths.get(localRepoPath, userGroupDir);
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(f -> f.toString().endsWith(".yaml"))
                    .map(f -> f.getFileName().toString().replace(".yaml", ""))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("getGroups error", e);
        }
    }

    public Optional<UserGroup> getUserGroup(String groupId) {
        Path file = Paths.get(localRepoPath, userGroupDir, groupId + ".yaml");
        if (!Files.exists(file)) return Optional.empty();

        try (InputStream in = Files.newInputStream(file)) {
            return Optional.ofNullable(mapper.readValue(in, UserGroup.class));
        } catch (Exception e) {
            throw new RuntimeException("getUserGroup error " + groupId, e);
        }
    }

    public List<String> getMembers(String groupId) {
        return getUserGroup(groupId)
                .map(UserGroup::getMembers)
                .orElse(Collections.emptyList());
    }

    public Map<String, List<String>> getAllGroupsWithMembers() {
        return getGroups().stream()
                .map(this::getUserGroup)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(UserGroup::getMemberGroup, UserGroup::getMembers));
    }
}
