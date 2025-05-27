package com.ns.secrets.command.service;

import com.ns.secrets.command.domain.User;

import java.util.List;
import java.util.Map;

public interface UserService {
    User getUser(String userId);
    List<String> getGroups();
    List<String> getMembers(String group);
    List<String> getUsersInGroup(String group);
    Map<String, List<String>> getAllGroupsWithMembers();
}
