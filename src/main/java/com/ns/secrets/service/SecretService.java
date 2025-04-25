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

    public void saveEncrypted(String userId, String key, String plainText) {
        String encrypted = aes.encrypt(plainText);
        saveRaw(userId, key, encrypted);
    }

    public String getDecrypted(String userId, String key) {
        String encrypted = etcd.get(path(userId, key));
        if (encrypted == null) return null;
        return aes.decrypt(encrypted);
    }

    public void saveRaw(String userId, String key, String value) {
        etcd.put(path(userId, key), value);
    }

    public String getRaw(String userId, String key) {
        return etcd.get(path(userId, key));
    }

    private String path(String userId, String key) {
        return "/secrets/" + userId + "/" + key;
    }


    // TLL을 지정해서 etcd에 저장
    public void saveWithTTL(String userId, String key, String plainText, long ttlSeconds) {
        long leaseId = etcd.grantLease(ttlSeconds);
        String encrypted = aes.encrypt(plainText);
        etcd.putWithLease(path(userId, key), encrypted, leaseId);
    }

    // test 너무 답답해서 만듬
    public List<String> getAllSecrets() {
        List<String> allKeys = etcd.getAllKeys("/secrets/");
        return allKeys.stream()
                .map(key -> {
                    String userId = key.split("/")[2];  // /secrets/{userId}/{key}
                    String secretKey = key.split("/")[3];
                    String decryptedSecret = getDecrypted(userId, secretKey);
                    return userId + " - " + secretKey + ": " + decryptedSecret;
                })
                .collect(Collectors.toList());
    }
}
