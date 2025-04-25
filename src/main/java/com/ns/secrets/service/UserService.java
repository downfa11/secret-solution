package com.ns.secrets.service;

import com.ns.secrets.domain.User;
import com.ns.secrets.domain.UserGroup;
import com.ns.secrets.repository.EtcdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserGroupService userGroupService;
    private final EtcdRepository etcdRepository;

    public void createUser(String userId) {
        String group = "default";
        userGroupService.setUserGroup(userId, group);
        System.out.println("User " + userId + " created with group " + group);
    }

    public void deleteUser(String userId) {
        String key = "/users/" + userId;
        etcdRepository.put(key, "");
        System.out.println("User " + userId + " deleted.");
    }

    public User getUser(String userId) {
        String group = userGroupService.getUserGroup(userId);
        return new User(userId, group);
    }

    public void bindPolicyToUser(String userId, String policyId) {
        String key = "/users/" + userId + "/policies/" + policyId;
        etcdRepository.put(key, "true");
    }
}
