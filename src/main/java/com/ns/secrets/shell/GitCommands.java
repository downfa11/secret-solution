package com.ns.secrets.shell;

import com.ns.secrets.service.GitService;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
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
        try {
            gitService.sync();
            return "Git repository synced with remote successfully.";
        } catch (GitAPIException e) {
            return "Failed to sync Git repository: " + e.getMessage();
        }
    }

    @ShellMethod("Rollback local git repo to specific commit hash")
    public String rollback(@ShellOption(help = "Commit hash to rollback to") String commitHash) {
        try {
            gitService.rollbackToCommit(commitHash);
            return "Rolled back to commit " + commitHash;
        } catch (GitAPIException e) {
            return "Rollback failed: " + e.getMessage();
        }
    }

    @ShellMethod("Checkout a specific branch in local git repo")
    public String branch(@ShellOption(help = "Branch name to checkout") String branchName) {
        try {
            gitService.checkoutBranch(branchName);
            return "Checked out branch " + branchName;
        } catch (GitAPIException e) {
            return "Branch checkout failed: " + e.getMessage();
        }
    }


    @ShellMethod("Get current HEAD commit hash")
    public String currentCommit() {
        try {
            return "Current commit: " + gitService.getCurrentCommit();
        } catch (RuntimeException e) {
            return "Failed to get current commit: " + e.getMessage();
        }
    }

    @ShellMethod("Show recent git commits")
    public String recentCommits(@ShellOption(defaultValue = "5") int count) {
        try {
            List<String> commits = gitService.getRecentCommitMessages(count);
            return String.join("\n", commits);
        } catch (Exception e) {
            return "Failed to get commits: " + e.getMessage();
        }
    }
}
