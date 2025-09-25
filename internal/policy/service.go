package policy

import (
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"strings"

	"secret-solution/internal/config"

	"gopkg.in/yaml.v2"
)

type Statement struct {
	Effect    string   `yaml:"effect"`
	Actions   []string `yaml:"actions"`
	Resources []string `yaml:"resources"`
}

type Policy struct {
	ID         string      `yaml:"id"`
	Version    string      `yaml:"version"`
	Statement  []Statement `yaml:"statement"`
	Name       string      `yaml:"name"`
	Allowed    []string    `yaml:"allowed"`
	Denied     []string    `yaml:"denied"`
	UserGroups []string    `yaml:"userGroups"`
}

type PolicyService struct {
	policiesDir string
}

func NewPolicyService(cfg *config.AppConfig) (*PolicyService, error) {
	policiesDir := filepath.Join(cfg.Git.LocalRepoPath, "policies")
	if _, err := os.Stat(policiesDir); os.IsNotExist(err) {
		return nil, fmt.Errorf("policies directory not found: %s", policiesDir)
	}

	return &PolicyService{policiesDir: policiesDir}, nil
}

func (s *PolicyService) GetPolicyByID(id string) (*Policy, error) {
	policyPath := filepath.Join(s.policiesDir, id+".yaml")

	yamlFile, err := ioutil.ReadFile(policyPath)
	if os.IsNotExist(err) {
		return nil, nil
	}
	if err != nil {
		return nil, fmt.Errorf("error reading policy file %s: %w", id, err)
	}

	var policy Policy
	if err := yaml.Unmarshal(yamlFile, &policy); err != nil {
		return nil, fmt.Errorf("error unmarshalling policy file %s: %w", id, err)
	}

	return &policy, nil
}

func (s *PolicyService) GetAllPolicies() ([]*Policy, error) {
	files, err := ioutil.ReadDir(s.policiesDir)
	if err != nil {
		return nil, fmt.Errorf("error reading policies directory: %w", err)
	}

	var policies []*Policy
	for _, file := range files {
		if strings.HasSuffix(file.Name(), ".yaml") {
			filePath := filepath.Join(s.policiesDir, file.Name())
			yamlFile, err := ioutil.ReadFile(filePath)
			if err != nil {
				continue
			}

			var policy Policy
			if err := yaml.Unmarshal(yamlFile, &policy); err != nil {
				continue
			}
			policies = append(policies, &policy)
		}
	}

	return policies, nil
}

func (s *PolicyService) GetPoliciesByIDs(policyIDs []string) ([]*Policy, error) {
	var policies []*Policy
	for _, id := range policyIDs {
		policy, err := s.GetPolicyByID(id)
		if err != nil {
			return nil, err
		}
		if policy != nil {
			policies = append(policies, policy)
		}
	}
	return policies, nil
}
