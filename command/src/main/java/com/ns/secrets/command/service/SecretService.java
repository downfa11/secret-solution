package com.ns.secrets.command.service;

import java.util.List;

public interface SecretService {
    String setKey(String newKey);
    void saveRaw(String executorUserId, String namespace, String key, String plainText);
    void saveRaw(String executorUserId, String namespace, String key, String plainText, boolean ttl, long ttlSeconds);
    void saveEncrypted(String executorUserId, String namespace, String key, String plainText);
    void saveEncrypted(String executorUserId, String namespace, String key, String plainText, boolean ttl, long ttlSeconds);
    String getDecrypted(String userId, String namespace, String key);
    String getRaw(String userId, String namespace, String key);
    List<String> getAllSecrets(String requesterUserId, String namespace);
}
