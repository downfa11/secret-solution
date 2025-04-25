package com.ns.secrets.controller;

import com.ns.secrets.service.GitService;
import com.ns.secrets.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GitController {
    private final GitService gitService;
    private final PolicyService policyService;

    @PostMapping("/sync")
    public void syncPoliciesFromGit() {
        policyService.syncPoliciesFromGit();
    }


    // 정책 잘못 적용한 경우, 해당 CommitHash로 상태를 되돌리는 메서드
    // CommitHash = git log로 확인할 수 있는 SHA-1 Hash.  ex) commit a1b2c3d4e5f6g7h8i9j0k123456789abcdef123
    @PostMapping("/rollback")
    public void rollbackToCommit(@RequestParam String commitHash) {
        try {
            gitService.rollbackToCommit(commitHash);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
