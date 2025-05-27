package com.ns.secrets.controller;

import com.ns.secrets.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/{userId}")
    public Map<String, String> getUser(@PathVariable String userId) {
        var user = userService.getUser(userId);
        return Map.of(
                "userId", userId,
                "group", user.getGroup()
        );
    }


    @GetMapping("/groups")
    public List<String> listGroups() {
        return userService.getGroups();
    }

    @GetMapping("/groups/{group}/members")
    public List<String> getGroupMembers(@PathVariable String group) {
        return userService.getMembers(group);
    }

    @GetMapping("/groups/{group}/users")
    public List<String> getUsersInGroup(@PathVariable String group) {
        return userService.getUsersInGroup(group);
    }

    @GetMapping("/groups-with-members")
    public Map<String, List<String>> getAllGroupsWithMembers() {
        return userService.getAllGroupsWithMembers();
    }
}
