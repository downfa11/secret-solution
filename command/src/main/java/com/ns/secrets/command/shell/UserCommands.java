package com.ns.secrets.command.shell;

import com.ns.secrets.command.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.util.List;

@ShellComponent
@RequiredArgsConstructor
public class UserCommands {
    private final UserService userService;

    @ShellMethod(key = "get-user", value = "Gets the user details")
    public String getUser(String userId) {
        var user = userService.getUser(userId);
        return "User " + userId + " belongs to group " + user.getGroup();
    }

    @ShellMethod(key = "list-groups", value = "List all user groups")
    public String listGroups() {
        List<String> groups = userService.getGroups();
        return "User groups: " + String.join(", ", groups);
    }

    @ShellMethod(key = "get-group-members", value = "Get members of a user group")
    public String getGroupMembers(String group) {
        List<String> members = userService.getMembers(group);
        if (members.isEmpty()) {
            return "No members found in group: " + group;
        }
        return "Members in group " + group + ": " + String.join(", ", members);
    }

    @ShellMethod(key = "get-users-in-group", value = "Gets the users in a specific group")
    public String getUsersInGroup(String group) {
        List<String> users = userService.getUsersInGroup(group);
        if (users.isEmpty()) {
            return "No users found in group " + group;
        }
        return "Users in group " + group + ": " + String.join(", ", users);
    }

    @ShellMethod(key = "get-all-groups", value = "Get all groups with their members")
    public String getAllGroupsWithMembers() {
        var groupMap = userService.getAllGroupsWithMembers();
        if (groupMap.isEmpty()) {
            return "No user groups found.";
        }

        StringBuilder sb = new StringBuilder();
        groupMap.forEach((group, members) -> {
            sb.append("Group: ").append(group).append("\n");
            sb.append("Members: ").append(String.join(", ", members)).append("\n");
            sb.append("---\n");
        });

        return sb.toString();
    }
}
