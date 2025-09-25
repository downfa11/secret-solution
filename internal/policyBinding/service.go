package policyBinding

import (
	"encoding/json"
	"fmt"
	"log"
	"secret-solution/internal/etcd"
	"strings"
)

type MemberType string

const (
	MemberTypeUser  MemberType = "user"
	MemberTypeGroup MemberType = "group"
)

type PolicyBinding struct {
	MemberID         string     `json:"memberId"`
	MemberType       MemberType `json:"memberType"`
	AttachedPolicies []string   `json:"attachedPolicies"`
}

type PolicyBindingService struct {
	etcdRepo etcd.EtcdRepository
}

func NewPolicyBindingService(etcdRepo etcd.EtcdRepository) *PolicyBindingService {
	return &PolicyBindingService{
		etcdRepo: etcdRepo,
	}
}

func (s *PolicyBindingService) BindPoliciesToMember(memberID string, memberType MemberType, policyIDs []string) error {
	policyBinding := PolicyBinding{
		MemberID:         memberID,
		MemberType:       memberType,
		AttachedPolicies: policyIDs,
	}

	key := s.buildKey(memberType, memberID)
	data, err := s.serialize(&policyBinding)
	if err != nil {
		return fmt.Errorf("failed to serialize policy binding: %w", err)
	}

	err = s.etcdRepo.Put(key, data)
	if err != nil {
		return fmt.Errorf("failed to put policy binding to etcd: %w", err)
	}
	log.Printf("Binding Policy %s: %s -> %v", memberType, memberID, policyIDs)
	return nil
}

func (s *PolicyBindingService) UnbindPoliciesFromMember(memberID string, memberType MemberType) error {
	key := s.buildKey(memberType, memberID)
	err := s.etcdRepo.Delete(key)
	if err != nil {
		return fmt.Errorf("failed to delete policy binding from etcd: %w", err)
	}
	log.Printf("Unbinding Policy %s: %s", memberType, memberID)
	return nil
}

func (s *PolicyBindingService) GetPolicyBindingForMember(memberID string, memberType MemberType) (*PolicyBinding, error) {
	key := s.buildKey(memberType, memberID)
	data, err := s.etcdRepo.Get(key)
	if err != nil {
		return nil, fmt.Errorf("failed to get policy binding from etcd: %w", err)
	}

	if data == "" {
		log.Printf("Not found %s: %s", memberType, memberID)
		return nil, nil
	}

	binding, err := s.deserialize(data)
	if err != nil {
		return nil, fmt.Errorf("failed to deserialize policy binding: %w", err)
	}
	log.Printf("GetPolicyBindingForMember %s: %s -> %v", memberType, memberID, binding.AttachedPolicies)
	return binding, nil
}

func (s *PolicyBindingService) UnbindPolicyFromAllMembers(policyID string) error {
	prefix := "/policy-bindings/"
	keys, err := s.etcdRepo.GetAllKeys(prefix)
	if err != nil {
		return fmt.Errorf("failed to get all keys from etcd: %w", err)
	}

	for _, key := range keys {
		data, err := s.etcdRepo.Get(key)
		if err != nil {
			log.Printf("Error getting data for key %s: %v", key, err)
			continue
		}
		if data == "" {
			continue
		}

		binding, err := s.deserialize(data)
		if err != nil {
			log.Printf("Error deserializing binding for key %s: %v", key, err)
			continue
		}

		var updatedPolicies []string
		for _, pID := range binding.AttachedPolicies {
			if pID != policyID {
				updatedPolicies = append(updatedPolicies, pID)
			}
		}

		if len(updatedPolicies) == len(binding.AttachedPolicies) {
			continue
		}

		if len(updatedPolicies) == 0 {
			if err := s.etcdRepo.Delete(key); err != nil {
				return fmt.Errorf("failed to delete empty policy binding: %w", err)
			}
			log.Printf("Empty policy binding %s: %s", binding.MemberType, binding.MemberID)
		} else {
			updatedBinding := PolicyBinding{
				MemberID:         binding.MemberID,
				MemberType:       binding.MemberType,
				AttachedPolicies: updatedPolicies,
			}
			data, err := s.serialize(&updatedBinding)
			if err != nil {
				return fmt.Errorf("failed to serialize updated binding: %w", err)
			}
			if err := s.etcdRepo.Put(key, data); err != nil {
				return fmt.Errorf("failed to update policy binding: %w", err)
			}
			log.Printf("Unbound policy %s: %s", binding.MemberType, binding.MemberID)
		}
	}
	return nil
}

func (s *PolicyBindingService) buildKey(memberType MemberType, memberID string) string {
	return fmt.Sprintf("/policy-bindings/%s/%s", strings.ToLower(string(memberType)), memberID)
}

func (s *PolicyBindingService) serialize(pb *PolicyBinding) (string, error) {
	data, err := json.Marshal(pb)
	if err != nil {
		return "", err
	}
	return string(data), nil
}

func (s *PolicyBindingService) deserialize(data string) (*PolicyBinding, error) {
	var pb PolicyBinding
	err := json.Unmarshal([]byte(data), &pb)
	if err != nil {
		return nil, err
	}
	return &pb, nil
}
