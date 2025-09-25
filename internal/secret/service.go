package secret

import (
	"fmt"
	"log"
	"secret-solution/internal/etcd"
	"secret-solution/internal/permission"
	"strings"
)

type SecretService struct {
	etcdRepo    etcd.EtcdRepository
	encryptor   AesEncryptor
	permService *permission.PermissionService
}

func NewSecretService(etcdRepo etcd.EtcdRepository, encryptor AesEncryptor, permService *permission.PermissionService) *SecretService {
	return &SecretService{
		etcdRepo:    etcdRepo,
		encryptor:   encryptor,
		permService: permService,
	}
}

func (s *SecretService) SaveRaw(executorUserID, namespace, key, value string, hasTTL bool, ttlSeconds int64) error {
	resource := s.resourcePath(namespace, key)
	if !s.permService.CheckPermission(executorUserID, "secret:write", resource) {
		return fmt.Errorf("security error: user %s not allowed to write on %s", executorUserID, resource)
	}

	etcdKey := s.path(namespace, key)
	if hasTTL && ttlSeconds > 0 {
		leaseID, err := s.etcdRepo.GrantLease(ttlSeconds)
		if err != nil {
			return fmt.Errorf("failed to grant lease: %w", err)
		}
		return s.etcdRepo.PutWithLease(etcdKey, value, leaseID)
	}
	return s.etcdRepo.Put(etcdKey, value)
}

func (s *SecretService) SaveEncrypted(executorUserID, namespace, key, plainText string, hasTTL bool, ttlSeconds int64) error {
	resource := s.resourcePath(namespace, key)
	if !s.permService.CheckPermission(executorUserID, "secret:encrypt", resource) {
		return fmt.Errorf("security error: user %s not allowed to encrypt on %s", executorUserID, resource)
	}

	encrypted, err := s.encryptor.Encrypt(plainText)
	if err != nil {
		return fmt.Errorf("failed to encrypt secret: %w", err)
	}

	etcdKey := s.path(namespace, key)
	if hasTTL && ttlSeconds > 0 {
		leaseID, err := s.etcdRepo.GrantLease(ttlSeconds)
		if err != nil {
			return fmt.Errorf("failed to grant lease: %w", err)
		}
		return s.etcdRepo.PutWithLease(etcdKey, encrypted, leaseID)
	}
	return s.etcdRepo.Put(etcdKey, encrypted)
}

func (s *SecretService) GetRaw(executorUserID, namespace, key string) (string, error) {
	resource := s.resourcePath(namespace, key)
	if !s.permService.CheckPermission(executorUserID, "secret:read", resource) {
		return "", fmt.Errorf("security error: user %s not allowed to read on %s", executorUserID, resource)
	}
	return s.etcdRepo.Get(s.path(namespace, key))
}

func (s *SecretService) GetDecrypted(executorUserID, namespace, key string) (string, error) {
	resource := s.resourcePath(namespace, key)
	if !s.permService.CheckPermission(executorUserID, "secret:decrypt", resource) {
		return "", fmt.Errorf("security error: user %s not allowed to decrypt on %s", executorUserID, resource)
	}

	encrypted, err := s.etcdRepo.Get(s.path(namespace, key))
	if err != nil {
		return "", fmt.Errorf("failed to retrieve encrypted secret: %w", err)
	}
	if encrypted == "" {
		return "", nil
	}
	return s.encryptor.Decrypt(encrypted)
}

func (s *SecretService) GetAllSecrets(executorUserID, namespace string) ([]string, error) {
	resource := fmt.Sprintf("/secrets/%s/*", namespace)
	if !s.permService.CheckPermission(executorUserID, "secret:read", resource) {
		return nil, fmt.Errorf("security error: user %s not allowed to read all secrets in %s", executorUserID, namespace)
	}

	prefix := fmt.Sprintf("/secrets/%s/", namespace)
	keys, err := s.etcdRepo.GetAllKeys(prefix)
	if err != nil {
		return nil, fmt.Errorf("failed to get all secret keys: %w", err)
	}

	secrets := make([]string, 0, len(keys))
	for _, key := range keys {
		decryptedSecret, err := s.parseKeyAndDecrypt(key)
		if err != nil {
			log.Printf("Error processing secret key %s: %v", key, err)
			continue
		}
		secrets = append(secrets, decryptedSecret)
	}
	return secrets, nil
}

func (s *SecretService) path(namespace, key string) string {
	return fmt.Sprintf("/secrets/%s/%s", namespace, key)
}

func (s *SecretService) resourcePath(namespace, key string) string {
	return fmt.Sprintf("secret/%s/%s", namespace, key)
}

func (s *SecretService) parseKeyAndDecrypt(fullKey string) (string, error) {
	parts := strings.Split(fullKey, "/")
	if len(parts) < 4 {
		return "", fmt.Errorf("invalid key in parseKeyAndDecrypt: %s", fullKey)
	}
	namespace := parts[2]
	key := parts[3]

	encrypted, err := s.etcdRepo.Get(fullKey)
	if err != nil {
		return "", fmt.Errorf("failed to get secret data for key %s: %w", fullKey, err)
	}

	decrypted, err := s.encryptor.Decrypt(encrypted)
	if err != nil {
		return "", fmt.Errorf("failed to decrypt secret for key %s: %w", fullKey, err)
	}

	return fmt.Sprintf("%s - %s: %s", namespace, key, decrypted), nil
}
