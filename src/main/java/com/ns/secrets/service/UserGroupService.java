package com.ns.secrets.service;

import com.ns.secrets.domain.User;
import com.ns.secrets.repository.EtcdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserGroupService {
    private final EtcdRepository etcdRepository;

    public void setUserGroup(String userId, String group) {
        String key = "/users/" + userId + "/group";
        etcdRepository.put(key, group);
    }

    public String getUserGroup(String userId) {
        String key = "/users/" + userId + "/group";
        return etcdRepository.get(key);
    }

    public void updateUserGroup(String userId, String newGroup) {
        setUserGroup(userId, newGroup);
        System.out.println("User " + userId + " group updated to " + newGroup);
    }

    public List<String> getUsersInGroup(String group) {
        return etcdRepository.getAllKeys("/users/")
                .stream()
                .filter(key -> etcdRepository.get(key).equals(group))
                .map(key -> key.split("/")[2])
                .collect(Collectors.toList());
    }
}
