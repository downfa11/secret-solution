package com.ns.secrets.command.service;

import java.util.List;

public interface GitService {
    String sync();
    String rollback(String commitHash);
    String branch(String branchName);
    String currentCommit();
    List<String> recentCommits(int count);
}

