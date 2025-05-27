package com.ns.secrets.command.shell;

import com.ns.secrets.command.service.GitService;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.List;

@ShellComponent
@RequiredArgsConstructor
public class GitCommands {

    private final GitService gitService;

    @ShellMethod("Sync local git repo with remote (git pull)")
    public String sync() {
        return gitService.sync();
    }

    @ShellMethod("Rollback local git repo to specific commit hash")
    public String rollback(@ShellOption(help = "Commit hash to rollback to") String commitHash) {
        return gitService.rollback(commitHash);
    }

    @ShellMethod("Checkout a specific branch in local git repo")
    public String branch(@ShellOption(help = "Branch name to checkout") String branchName) {
        return gitService.branch(branchName);
    }

    @ShellMethod("Get current HEAD commit hash")
    public String currentCommit() {
        return gitService.currentCommit();
    }

    @ShellMethod("Show recent git commits")
    public String recentCommits(@ShellOption(defaultValue = "5", help = "Number of commits to show") int count) {
        List<String> commits = gitService.recentCommits(count);
        return String.join("\n", commits);
    }
}
