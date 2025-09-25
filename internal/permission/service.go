package permission

import (
	"log"
	"secret-solution/internal/policy"
	"secret-solution/internal/policyBinding"
	"secret-solution/internal/user"
	"strings"
)

type PermissionService struct {
	policyService        *policy.PolicyService
	policyBindingService *policyBinding.PolicyBindingService
	userService          *user.UserService
}

func NewPermissionService(policyService *policy.PolicyService, policyBindingService *policyBinding.PolicyBindingService, userService *user.UserService) *PermissionService {
	return &PermissionService{
		policyService:        policyService,
		policyBindingService: policyBindingService,
		userService:          userService,
	}
}

func (s *PermissionService) CheckPermission(userID string, actions []string, resource string) bool {
	userBinding, err := s.policyBindingService.GetPolicyBindingForMember(userID, policyBinding.MemberTypeUser)
	if err != nil {
		log.Printf("Error getting user policy binding for %s: %v", userID, err)
		return false
	}

	group, err := s.userService.GetGroupForUser(userID)
	if err != nil {
		log.Printf("Error getting group for user %s: %v", userID, err)
		return false
	}

	var groupBinding *policyBinding.PolicyBinding
	if group != "" {
		groupBinding, err = s.policyBindingService.GetPolicyBindingForMember(group, policyBinding.MemberTypeGroup)
		if err != nil {
			log.Printf("Error getting group policy binding for %s: %v", group, err)
			return false
		}
	}

	var policies []*policy.Policy
	if userBinding != nil && len(userBinding.AttachedPolicies) > 0 {
		userPolicies, err := s.policyService.GetPoliciesByIDs(userBinding.AttachedPolicies)
		if err != nil {
			log.Printf("Error getting user policies: %v", err)
			return false
		}
		policies = append(policies, userPolicies...)
	}

	if groupBinding != nil && len(groupBinding.AttachedPolicies) > 0 {
		groupPolicies, err := s.policyService.GetPoliciesByIDs(groupBinding.AttachedPolicies)
		if err != nil {
			log.Printf("Error getting group policies: %v", err)
			return false
		}
		policies = append(policies, groupPolicies...)
	}

	for _, action := range actions {
		allowed := false
		for _, p := range policies {
			for _, stmt := range p.Statement {
				if strings.EqualFold(stmt.Effect, "allow") {
					actionMatch := false
					for _, a := range stmt.Actions {
						if a == action {
							actionMatch = true
							break
						}
					}
					resourceMatch := false
					for _, r := range stmt.Resources {
						if match(resource, r) {
							resourceMatch = true
							break
						}
					}
					if actionMatch && resourceMatch {
						allowed = true
						break
					}
				}
			}
			if allowed {
				break
			}
		}
		if !allowed {
			log.Printf("Permission denied for user %s on resource %s, missing action %s", userID, resource, action)
			return false
		}
	}

	log.Printf("Permission granted for user %s on resource %s for actions %v", userID, resource, actions)
	return true
}

func match(actual, pattern string) bool {
	if strings.HasSuffix(pattern, "/*") {
		prefix := strings.TrimSuffix(pattern, "/*")
		return strings.HasPrefix(actual, prefix)
	}
	return actual == pattern
}
