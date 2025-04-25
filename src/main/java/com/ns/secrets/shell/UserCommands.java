package com.ns.secrets.shell;

import com.ns.secrets.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
@RequiredArgsConstructor
public class UserCommands {

    private final UserService userService;

    @ShellMethod(key = "create-user", value = "Creates a new user")
    public String createUser(String userId) {
        userService.createUser(userId);
        return "User " + userId + " created successfully.";
    }

    @ShellMethod(key = "delete-user", value = "Deletes an existing user")
    public String deleteUser(String userId) {
        userService.deleteUser(userId);
        return "User " + userId + " deleted successfully.";
    }

    @ShellMethod(key = "get-user", value = "Gets the user details")
    public String getUser(String userId) {
        var user = userService.getUser(userId);
        return "User " + userId + " belongs to group " + user.getGroup();
    }
}
