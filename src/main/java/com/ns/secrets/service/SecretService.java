package com.ns.secrets.service;

import com.ns.secrets.repository.EtcdRepository;
import com.ns.secrets.utils.AesEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SecretService {

    private final EtcdRepository etcd;
    private final AesEncryptor aes;
    private final PermissionService permission;


    public void saveRaw(String executorUserId, String namespace, String key, String value) {
        saveRaw(executorUserId, namespace, key, value, false, 0L);
    }

    public void saveRaw(String executorUserId, String namespace, String key, String value, boolean hasTTL, long ttlSeconds) {
        checkPermission(executorUserId, "secret:write", resourcePath(namespace, key));

        if (hasTTL) {
            long leaseId = etcd.grantLease(ttlSeconds);
            etcd.putWithLease(path(namespace, key), value, leaseId);
        } else {
            etcd.put(path(namespace, key), value);
        }
    }

    public String getRaw(String executorUserId, String namespace, String key) {
        checkPermission(executorUserId, "secret:read", resourcePath(namespace, key));
        return etcd.get(path(namespace, key));
    }

    public void saveEncrypted(String executorUserId, String namespace, String key, String plainText) {
        saveEncrypted(executorUserId, namespace, key, plainText, false, 0L);
    }

    public void saveEncrypted(String executorUserId, String namespace, String key, String plainText, boolean hasTTL, long ttlSeconds) {
        checkPermission(executorUserId, "secret:encrypt", resourcePath(namespace, key));
        String encrypted = aes.encrypt(plainText);
        if (hasTTL) {
            long leaseId = etcd.grantLease(ttlSeconds);
            etcd.putWithLease(path(namespace, key), encrypted, leaseId);
        } else {
            etcd.put(path(namespace, key), encrypted);
        }
    }

    public String getDecrypted(String executorUserId, String namespace, String key) {
        checkPermission(executorUserId, "secret:decrypt", resourcePath(namespace, key));
        String encrypted = etcd.get(path(namespace, key));
        if (encrypted == null) return null;
        return aes.decrypt(encrypted);
    }

    public List<String> getAllSecrets(String executorUserId, String namespace) {
        checkPermission(executorUserId, "secret:read", "/secrets/" + namespace + "/*");
        return etcd.getAllKeys("/secrets/" + namespace + "/").stream()
                .map(this::parseKeyAndDecrypt)
                .collect(Collectors.toList());
    }


    // --- private methods ---
    private void checkPermission(String userId, String action, String resource) {
        if (!permission.isAllowed(userId, action, resource)) {
            throw new SecurityException("Not allowed to " + action + " on resource " + resource);
        }
    }

    private String parseKeyAndDecrypt(String fullKey) {
        String[] parts = fullKey.split("/");
        if (parts.length < 4) return "Invalid key in parseKeyAndDecrypt: " + fullKey;
        String namespace = parts[2]; // /secrets/{namespace}/{key}
        String key = parts[3];
        String decrypted = aes.decrypt(etcd.get(fullKey));
        return namespace + " - " + key + ": " + decrypted;
    }

    private String path(String userId, String key) {
        return "/secrets/" + userId + "/" + key;
    }

    private String resourcePath(String userId, String key) {
        return "secret/" + userId + "/" + key;
    }
}
