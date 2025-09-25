package user

import (
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"secret-solution/internal/config"
	"strings"

	"gopkg.in/yaml.v2"
)

type User struct {
	ID    string `yaml:"id"`
	Group string `yaml:"group"`
}

type UserGroup struct {
	MemberGroup string   `yaml:"memberGroup"`
	Members     []string `yaml:"members"`
}

type UserService struct {
	usersDir     string
	userGroupDir string
}

func NewUserService(cfg *config.AppConfig) (*UserService, error) {
	repoPath := cfg.Git.LocalRepoPath
	usersDir := filepath.Join(repoPath, "users")
	userGroupDir := filepath.Join(repoPath, "user-groups")

	if _, err := os.Stat(usersDir); os.IsNotExist(err) {
		return nil, fmt.Errorf("users directory not found: %s", usersDir)
	}
	if _, err := os.Stat(userGroupDir); os.IsNotExist(err) {
		return nil, fmt.Errorf("user groups directory not found: %s", userGroupDir)
	}

	return &UserService{
		usersDir:     usersDir,
		userGroupDir: userGroupDir,
	}, nil
}

func (s *UserService) loadUsers() (map[string]User, error) {
	files, err := ioutil.ReadDir(s.usersDir)
	if err != nil {
		return nil, fmt.Errorf("error reading users directory: %w", err)
	}

	users := make(map[string]User)
	for _, file := range files {
		if strings.HasSuffix(file.Name(), ".yaml") {
			filePath := filepath.Join(s.usersDir, file.Name())
			yamlFile, err := ioutil.ReadFile(filePath)
			if err != nil {
				return nil, fmt.Errorf("error reading user file %s: %w", file.Name(), err)
			}

			var user User
			if err := yaml.Unmarshal(yamlFile, &user); err != nil {
				return nil, fmt.Errorf("error unmarshalling user file %s: %w", file.Name(), err)
			}

			if user.ID != "" {
				users[user.ID] = user
			}
		}
	}
	return users, nil
}

func (s *UserService) GetUser(id string) (*User, error) {
	users, err := s.loadUsers()
	if err != nil {
		return nil, err
	}

	user, ok := users[id]
	if !ok {
		return nil, fmt.Errorf("user not found: %s", id)
	}
	return &user, nil
}

func (s *UserService) GetGroupForUser(id string) (string, error) {
	user, err := s.GetUser(id)
	if err != nil {
		return "", err
	}
	return user.Group, nil
}

func (s *UserService) GetUsersInGroup(group string) ([]string, error) {
	users, err := s.loadUsers()
	if err != nil {
		return nil, err
	}

	var userIDs []string
	for _, user := range users {
		if user.Group == group {
			userIDs = append(userIDs, user.ID)
		}
	}
	return userIDs, nil
}

func (s *UserService) GetGroups() ([]string, error) {
	files, err := ioutil.ReadDir(s.userGroupDir)
	if err != nil {
		return nil, fmt.Errorf("error reading user group directory: %w", err)
	}

	var groups []string
	for _, file := range files {
		if strings.HasSuffix(file.Name(), ".yaml") {
			groupName := strings.TrimSuffix(file.Name(), ".yaml")
			groups = append(groups, groupName)
		}
	}
	return groups, nil
}

func (s *UserService) GetUserGroup(groupID string) (*UserGroup, error) {
	filePath := filepath.Join(s.userGroupDir, groupID+".yaml")

	yamlFile, err := ioutil.ReadFile(filePath)
	if os.IsNotExist(err) {
		return nil, nil
	}
	if err != nil {
		return nil, fmt.Errorf("error reading user group file %s: %w", groupID, err)
	}

	var userGroup UserGroup
	if err := yaml.Unmarshal(yamlFile, &userGroup); err != nil {
		return nil, fmt.Errorf("error unmarshalling user group file %s: %w", groupID, err)
	}
	return &userGroup, nil
}

func (s *UserService) GetMembers(groupID string) ([]string, error) {
	group, err := s.GetUserGroup(groupID)
	if err != nil {
		return nil, err
	}
	if group == nil {
		return []string{}, nil
	}
	return group.Members, nil
}

func (s *UserService) GetAllGroupsWithMembers() (map[string][]string, error) {
	groups, err := s.GetGroups()
	if err != nil {
		return nil, err
	}

	allGroups := make(map[string][]string)
	for _, groupID := range groups {
		group, err := s.GetUserGroup(groupID)
		if err != nil {
			return nil, err
		}
		if group != nil {
			allGroups[group.MemberGroup] = group.Members
		}
	}
	return allGroups, nil
}
