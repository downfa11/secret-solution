package git

import (
	"fmt"
	"io"
	"log"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"

	"github.com/go-git/go-git/v5"
	"github.com/go-git/go-git/v5/plumbing"
	"github.com/go-git/go-git/v5/plumbing/object"
	"github.com/go-git/go-git/v5/plumbing/transport/http"
)

type GitService struct {
	repoPath         string
	repo             *git.Repository
	mu               sync.Mutex
	lastSyncedCommit string
}

func NewGitService(repoURL, localRepoPath string) (*GitService, error) {
	repoDir := filepath.Clean(localRepoPath)

	if _, err := os.Stat(filepath.Join(repoDir, ".git")); os.IsNotExist(err) {
		log.Printf("Git repository not found '%s'. Cloning from %s.", repoDir, repoURL)

		retries := 3
		for i := 0; i < retries; i++ {
			_, err := git.PlainClone(repoDir, false, &git.CloneOptions{
				URL:      repoURL,
				Progress: os.Stdout,
				Auth:     &http.BasicAuth{},
			})
			if err != nil {
				log.Printf("Failed to clone repository. Retrying... (attempt %d/%d)", i+1, retries)
				time.Sleep(2 * time.Second)
				if i == retries-1 {
					return nil, fmt.Errorf("git clone failed after %d attempts: %w", retries, err)
				}
			} else {
				break
			}
		}
	} else if err != nil {
		return nil, fmt.Errorf("failed to check repository status: %w", err)
	}

	repo, err := git.PlainOpen(repoDir)
	if err != nil {
		return nil, fmt.Errorf("failed to open repository: %w", err)
	}

	svc := &GitService{
		repoPath: repoDir,
		repo:     repo,
	}

	head, err := svc.repo.Head()
	if err == nil {
		svc.lastSyncedCommit = head.Hash().String()
	}

	log.Printf("Repository found at '%s'", localRepoPath)
	return svc, nil
}

func (s *GitService) Sync() ([]string, error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	oldHead, err := s.repo.Head()
	if err != nil {
		return nil, fmt.Errorf("failed to get HEAD commit: %w", err)
	}
	oldHash := oldHead.Hash()

	wt, err := s.repo.Worktree()
	if err != nil {
		return nil, fmt.Errorf("failed to get worktree: %w", err)
	}
	err = wt.Pull(&git.PullOptions{
		Auth:     &http.BasicAuth{},
		Progress: os.Stdout,
	})
	if err != nil && err != git.NoErrAlreadyUpToDate {
		return nil, fmt.Errorf("git pull failed: %w", err)
	}

	newHead, err := s.repo.Head()
	if err != nil {
		return nil, fmt.Errorf("failed to get new HEAD commit: %w", err)
	}
	newHash := newHead.Hash()

	if oldHash.String() == newHash.String() {
		log.Println("No changes after pull.")
		return nil, nil
	}

	s.lastSyncedCommit = newHash.String()

	oldCommit, err := s.repo.CommitObject(oldHash)
	if err != nil {
		return nil, fmt.Errorf("failed to get old commit object: %w", err)
	}
	newCommit, err := s.repo.CommitObject(newHash)
	if err != nil {
		return nil, fmt.Errorf("failed to get new commit object: %w", err)
	}

	patch, err := oldCommit.Patch(newCommit)
	if err != nil {
		return nil, fmt.Errorf("failed to generate patch: %w", err)
	}

	changedPolicies := make([]string, 0)
	for _, filePatch := range patch.FilePatches() {
		fromFile, toFile := filePatch.Files()
		var path string
		if toFile != nil {
			path = toFile.Path()
		} else if fromFile != nil {
			path = fromFile.Path()
		}

		if strings.HasPrefix(path, "policies/") && strings.HasSuffix(path, ".yaml") {
			changedPolicies = append(changedPolicies, path)
		}
	}

	if len(changedPolicies) > 0 {
		log.Printf("Policy updated: %v", changedPolicies)
	} else {
		log.Println("No policy changes found.")
	}

	return changedPolicies, nil
}

func (s *GitService) RollbackToCommit(commitHash string) error {
	s.mu.Lock()
	defer s.mu.Unlock()

	wt, err := s.repo.Worktree()
	if err != nil {
		return fmt.Errorf("failed to get worktree: %w", err)
	}
	hash := plumbing.NewHash(commitHash)
	err = wt.Reset(&git.ResetOptions{
		Mode:   git.HardReset,
		Commit: hash,
	})
	if err != nil {
		return fmt.Errorf("git reset failed: %w", err)
	}

	err = wt.Clean(&git.CleanOptions{})
	if err != nil {
		return fmt.Errorf("git clean failed: %w", err)
	}

	s.lastSyncedCommit = commitHash
	log.Printf("Rollback to commit %s completed.", commitHash)
	return nil
}

func (s *GitService) CheckoutBranch(branchName string) error {
	s.mu.Lock()
	defer s.mu.Unlock()

	wt, err := s.repo.Worktree()
	if err != nil {
		return fmt.Errorf("failed to get worktree: %w", err)
	}

	err = wt.Checkout(&git.CheckoutOptions{
		Branch: plumbing.NewBranchReferenceName(branchName),
		Force:  true,
	})
	if err != nil {
		return fmt.Errorf("git checkout failed: %w", err)
	}

	head, err := s.repo.Head()
	if err != nil {
		return fmt.Errorf("failed to get HEAD after checkout: %w", err)
	}
	s.lastSyncedCommit = head.Hash().String()
	log.Printf("Checkout to branch %s", branchName)
	return nil
}

func (s *GitService) CommitAndPush(message string) error {
	s.mu.Lock()
	defer s.mu.Unlock()

	wt, err := s.repo.Worktree()
	if err != nil {
		return fmt.Errorf("failed to get worktree: %w", err)
	}

	_, err = wt.Add(".")
	if err != nil {
		return fmt.Errorf("git add failed: %w", err)
	}

	commitHash, err := wt.Commit(message, &git.CommitOptions{
		Author: &object.Signature{
			Name:  "Secret Solution",
			Email: "info@example.com",
			When:  time.Now(),
		},
	})
	if err != nil {
		return fmt.Errorf("git commit failed: %w", err)
	}

	err = s.repo.Push(&git.PushOptions{
		Auth:     &http.BasicAuth{},
		Progress: os.Stdout,
	})
	if err != nil {
		return fmt.Errorf("git push failed: %w", err)
	}

	s.lastSyncedCommit = commitHash.String()
	log.Printf("commitAndPush completed: %s", message)
	return nil
}

func (s *GitService) GetRecentCommitMessages(count int) ([]string, error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	cIter, err := s.repo.Log(&git.LogOptions{
		Order: git.LogOrderCommitterTime,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to get commit log: %w", err)
	}

	var commitMessages []string
	var i int
	err = cIter.ForEach(func(c *object.Commit) error {
		if i >= count {
			return io.EOF
		}
		message := fmt.Sprintf("Commit: %s\nAuthor: %s <%s>\nDate: %s\nMessage: %s",
			c.Hash.String(), c.Author.Name, c.Author.Email, c.Author.When.Format(time.RFC3339), c.Message)
		commitMessages = append(commitMessages, message)
		i++
		return nil
	})
	if err != nil && err != io.EOF {
		return nil, fmt.Errorf("failed to iterate commit log: %w", err)
	}

	return commitMessages, nil
}

func (s *GitService) GetCurrentCommit() (string, error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	head, err := s.repo.Head()
	if err != nil {
		return "", fmt.Errorf("failed to get HEAD: %w", err)
	}

	return head.Hash().String(), nil
}
