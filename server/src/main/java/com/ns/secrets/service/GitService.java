package com.ns.secrets.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class GitService {

    @Value("${git.repo.url}")
    private String repoUrl;

    @Value("${git.repo.local-path}")
    private String localRepoPath;


    private final ReentrantLock gitLock = new ReentrantLock();
    private volatile String lastSyncedCommit = null;

    @PostConstruct
    public void init() {
        File repoDir = new File(localRepoPath);
        File gitDir = new File(localRepoPath, ".git");

        if (!repoDir.exists() || !gitDir.exists()) {
            int retries = 3;
            while (retries-- > 0) {
                try {
                    log.info("Git repository not found '{}'. Cloning repository from {}. Attempts: {}", localRepoPath, repoUrl, retries + 1);
                    cloneRepository();
                    log.info("Repository cloned successfully.");
                    return;
                } catch (GitAPIException e) {
                    log.error("Failed to cloned.", e);
                    if (retries == 0) {
                        throw new RuntimeException("Git init failed", e);
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Git thread failed", ie);
                    }
                }
            }
        } else {
            log.info("repository found at '{}'", localRepoPath);
        }
    }

    private File getRepoDir() {
        return new File(localRepoPath);
    }

    public void cloneRepository() throws GitAPIException {
        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(new File(localRepoPath))
                .call();
    }

    /**
     * policy 파일만 동기화 감지.
     * 동시성 안전하게 synchronized 처리,
     * 변경된 커밋을 캐싱해 불필요한 pull 및 diff 방지.
     */
    public List<String> sync() throws GitAPIException {
        gitLock.lock();
        try (Git git = Git.open(getRepoDir())) {
            String currentBranch = git.getRepository().getBranch();
            ObjectId oldHead = lastSyncedCommit == null ?
                    git.getRepository().resolve("HEAD") :
                    git.getRepository().resolve(lastSyncedCommit);

            PullResult pullResult = git.pull()
                    .setRemote("origin")
                    .setRemoteBranchName(currentBranch)
                    .call();

            ObjectId newHead = git.getRepository().resolve("HEAD");

            if (oldHead != null && !oldHead.equals(newHead)) {
                lastSyncedCommit = newHead.getName();

                List<String> changedFiles = listChangedFiles(git.getRepository(), oldHead, newHead);
                List<String> changedPolicies = changedFiles.stream()
                        .filter(path -> path.startsWith("policies/") && path.endsWith(".yaml"))
                        .toList();

                if (!changedPolicies.isEmpty()) {
                    log.info("Policy updated: {}", changedPolicies);
                    return changedPolicies;
                } else {
                    log.info("Not changes");
                    return List.of();
                }
            } else { // HEAD unchanged
                log.info("No changes after pull");
                return List.of();
            }
        } catch (IOException e) {
            log.error("Git sync IOException", e);
            throw new IllegalStateException("Git sync IOException", e);
        } finally {
            gitLock.unlock();
        }
    }


    private List<String> listChangedFiles(Repository repo, ObjectId oldHead, ObjectId newHead) throws IOException, GitAPIException {
        try (ObjectReader reader = repo.newObjectReader()) {
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            ObjectId oldTree = repo.resolve(oldHead.getName() + "^{tree}");
            if (oldTree == null) {
                throw new IOException("Not found in commit " + oldHead.getName());
            }
            oldTreeIter.reset(reader, oldTree);

            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            ObjectId newTree = repo.resolve(newHead.getName() + "^{tree}");
            if (newTree == null) {
                throw new IOException("Not found in commit " + newHead.getName());
            }
            newTreeIter.reset(reader, newTree);

            try (Git git = new Git(repo)) {
                return git.diff()
                        .setOldTree(oldTreeIter)
                        .setNewTree(newTreeIter)
                        .call()
                        .stream()
                        .map(diff -> diff.getNewPath())
                        .toList();
            }
        }
    }

    /**
     * 특정 커밋 해시로 hard reset + clean 수행
     * 락으로 동시성 안전 보장
     */
    public void rollbackToCommit(String commitHash) throws GitAPIException {
        gitLock.lock();
        try (Git git = Git.open(getRepoDir())) {
            git.reset()
                    .setMode(ResetCommand.ResetType.HARD)
                    .setRef(commitHash)
                    .call();
            git.clean()
                    .setCleanDirectories(true)
                    .call();
            lastSyncedCommit = commitHash;
            log.info("Rollback to commit {} completed.", commitHash);
        } catch (IOException e) {
            log.error("Failed to rollback.", e);
            throw new RuntimeException(e);
        } finally {
            gitLock.unlock();
        }
    }

    /**
     * 브랜치 체크아웃 (락 처리)
     */
    public void checkoutBranch(String branchName) throws GitAPIException {
        gitLock.lock();
        try (Git git = Git.open(getRepoDir())) {
            git.checkout().setName(branchName).call();
            lastSyncedCommit = git.getRepository().resolve("HEAD").getName();
            log.info("Checkout to branch {}", branchName);
        } catch (IOException e) {
            log.error("Failed to checkout.", e);
            throw new RuntimeException(e);
        } finally {
            gitLock.unlock();
        }
    }

    /**
     * commit 및 push는 별도 서비스로 분리 권고
     * deprecated 표시 유지
     */
    @Deprecated
    public void commitAndPush(String message) {
        gitLock.lock();
        try (Git git = Git.open(getRepoDir())) {
            git.add().addFilepattern(".").call();
            git.commit().setMessage(message).call();
            git.push().call();
            lastSyncedCommit = git.getRepository().resolve("HEAD").getName();

            log.info("commitAndPush: {}", message);
        } catch (GitAPIException | IOException e) {
            log.error("commitAndPush error: ", e);
            throw new RuntimeException("commitAndPush failed", e);
        } finally {
            gitLock.unlock();
        }
    }

    /**
     * 최근 커밋 메시지 조회
     */
    public List<String> getRecentCommitMessages(int count) {
        List<String> commitMessages = new ArrayList<>();
        gitLock.lock();
        try (Git git = Git.open(getRepoDir())) {
            Iterable<RevCommit> logs = git.log().setMaxCount(count).call();
            for (RevCommit logEntry : logs) {
                String message = String.format("Commit: %s\nMessage: %s\n---",
                        logEntry.getName(), logEntry.getFullMessage());
                commitMessages.add(message);
            }
        } catch (Exception e) {
            log.error("getRecentCommitMessages error", e);
            throw new RuntimeException("getRecentCommitMessages error", e);
        } finally {
            gitLock.unlock();
        }
        return commitMessages;
    }

    /**
     * 현재 HEAD 커밋 조회
     */
    public String getCurrentCommit() {
        gitLock.lock();
        try (Repository repository = new FileRepositoryBuilder()
                .setGitDir(new File(localRepoPath, ".git"))
                .build()) {
            ObjectId head = repository.resolve("HEAD");
            if (head == null) {
                String msg = "HEAD not found";
                log.error(msg);
                throw new RuntimeException(msg);
            }
            return head.getName();
        } catch (Exception e) {
            log.error("getCurrentCommit error: ", e);
            throw new RuntimeException("getCurrentCommit error", e);
        } finally {
            gitLock.unlock();
        }
    }
}