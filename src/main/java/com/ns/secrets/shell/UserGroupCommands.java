package com.ns.secrets.shell;

import com.ns.secrets.service.UserGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.util.List;

@ShellComponent
@RequiredArgsConstructor
public class UserGroupCommands {

    private final UserGroupService userGroupService;

    @ShellMethod(key = "set-user-group", value = "Sets a user's group")
    public String setUserGroup(String userId, String group) {
        userGroupService.setUserGroup(userId, group);
        return "User " + userId + " group set to " + group;
    }

    @ShellMethod(key = "get-user-group", value = "Gets the group of a user")
    public String getUserGroup(String userId) {
        String group = userGroupService.getUserGroup(userId);
        return "User " + userId + " belongs to group " + group;
    }

    @ShellMethod(key = "update-user-group", value = "Updates a user's group")
    public String updateUserGroup(String userId, String newGroup) {
        userGroupService.updateUserGroup(userId, newGroup);
        return "User " + userId + " group updated to " + newGroup;
    }

    @ShellMethod(key = "get-users-in-group", value = "Gets the users in a specific group")
    public String getUsersInGroup(String group) {
        List<String> users = userGroupService.getUsersInGroup(group);
        if (users.isEmpty()) {
            return "No users found in group " + group;
        }
        return "Users in group " + group + ": " + String.join(", ", users);
    }
}
