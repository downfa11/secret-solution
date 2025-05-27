package com.ns.secrets.controller;

import com.ns.secrets.service.GitService;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/git")
@RequiredArgsConstructor
public class GitController {

    private final GitService gitService;

    @PostMapping("/sync")
    public ResponseEntity<String> sync() {
        try {
            gitService.sync();
            return ResponseEntity.ok("Git repository synced with remote successfully.");
        } catch (GitAPIException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to sync Git repository: " + e.getMessage());
        }
    }

    @PostMapping("/rollback")
    public ResponseEntity<String> rollback(@RequestParam String commitHash) {
        try {
            gitService.rollbackToCommit(commitHash);
            return ResponseEntity.ok("Rolled back to commit " + commitHash);
        } catch (GitAPIException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Rollback failed: " + e.getMessage());
        }
    }

    @PostMapping("/branch")
    public ResponseEntity<String> branch(@RequestParam String branchName) {
        try {
            gitService.checkoutBranch(branchName);
            return ResponseEntity.ok("Checked out branch " + branchName);
        } catch (GitAPIException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Branch checkout failed: " + e.getMessage());
        }
    }

    @GetMapping("/current-commit")
    public ResponseEntity<String> currentCommit() {
        try {
            return ResponseEntity.ok("Current commit: " + gitService.getCurrentCommit());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get current commit: " + e.getMessage());
        }
    }

    @GetMapping("/recent-commits")
    public ResponseEntity<List<String>> recentCommits(@RequestParam(defaultValue = "5") int count) {
        try {
            return ResponseEntity.ok(gitService.getRecentCommitMessages(count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

