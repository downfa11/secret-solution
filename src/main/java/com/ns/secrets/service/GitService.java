package com.ns.secrets.service;

import jakarta.annotation.PostConstruct;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class GitService {
    @Value("${git.repo.url}")
    private String repoUrl;

    @Value("${git.repo.local-path}")
    private String localRepoPath;

    @PostConstruct
    public void init() {
        File repoDirectory = new File(localRepoPath);

        if (!repoDirectory.exists() || !repoDirectory.isDirectory()) {
            try {
                System.out.println("Repository not found");
                cloneRepository();
            } catch (GitAPIException e) {
                throw new RuntimeException("checkAndCloneRepository error ", e);
            }
        }
    }

    public void cloneRepository() throws GitAPIException {
        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(new File(localRepoPath))
                .call();
    }

    // Git Repository에서 Policy fetch
    public void syncPolicyFile() throws GitAPIException {
        try (Git git = Git.open(new File(localRepoPath))) {
            git.pull().call();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Git Commit을 사용하여 rollback
    public void rollbackToCommit(String commitHash) throws GitAPIException {
        try (Git git = Git.open(new File(localRepoPath))) {
            git.checkout().setName(commitHash).call();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
