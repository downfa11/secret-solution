package com.ns.secrets.shell;

import com.ns.secrets.service.SecretService;
import com.ns.secrets.utils.AesEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@ShellComponent
@RequiredArgsConstructor
public class SecretCommands {
    private final AesEncryptor aesEncryptor;
    private final SecretService secretService;


    @ShellMethod(key = "generate-key", value = "Generates a new AES key")
    public String generateAesKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    @ShellMethod(key = "set-key", value = "Sets a new AES key")
    public String setAesKey(String newKey) {
        try {
            aesEncryptor.setKey(newKey);
            return "AES key updated successfully.";
        } catch (Exception e) {
            return "Failed to update AES key: " + e.getMessage();
        }
    }

    @ShellMethod(key = "put", value = "Saves the secret as raw plaintext without encryption")
    public String saveRaw(String executorUserId, String namespace, String key, String plainText) {
        secretService.saveRaw(executorUserId, namespace, key, plainText);
        return "Raw secret saved in namespace " + namespace + " with key " + key;
    }

    @ShellMethod(key = "put-raw-ttl", value = "Saves the secret as raw plaintext without encryption")
    public String saveRawWithTTL(String executorUserId, String namespace, String key, String plainText, long ttlSeconds) {
        secretService.saveRaw(executorUserId, namespace, key, plainText, true, ttlSeconds);
        return "Raw secret saved in namespace " + namespace + " with key " + key;
    }

    @ShellMethod(key = "encrypted", value = "Encrypts and saves the secret in a namespace")
    public String saveEncrypted(String executorUserId, String namespace, String key, String plainText) {
        secretService.saveEncrypted(executorUserId, namespace, key, plainText);
        return "Encrypted secret saved in namespace " + namespace + " with key " + key;
    }

    @ShellMethod(key = "encrypted-ttl", value = "Encrypts and saves the secret in a namespace with TTL")
    public String saveEncryptedWithTTL(String executorUserId, String namespace, String key, String plainText, long ttlSeconds) {
        secretService.saveEncrypted(executorUserId, namespace, key, plainText, true, ttlSeconds);
        return "Encrypted secret with TTL saved in namespace " + namespace + " with key " + key + ", TTL: " + ttlSeconds + "s";
    }

    @ShellMethod(key = "decrypted", value = "Fetches and decrypts the secret")
    public String getDecrypted(String userId, String namespace, String key) {
        String decrypted = secretService.getDecrypted(userId, namespace, key);
        if (decrypted == null) {
            return "No secret found for user " + userId + " with key " + key;
        }
        return "Decrypted secret for user " + userId + " with key " + key + ": " + decrypted;
    }

    @ShellMethod(key = "get", value = "Fetches the raw secret")
    public String getRaw(String userId, String namespace, String key) {
        String raw = secretService.getRaw(userId, namespace, key);
        if (raw == null) {
            return "No raw secret found for user " + userId + " with key " + key;
        }
        return "Raw secret for user " + userId + " with key " + key + ": " + raw;
    }

    @ShellMethod(key = "list-secrets", value = "Lists all stored secrets")
    public String listSecrets(String requesterUserId, String namespace) {
        List<String> allSecrets = secretService.getAllSecrets(requesterUserId, namespace);
        if (allSecrets.isEmpty()) {
            return "No secrets found.";
        }
        return String.join("\n", allSecrets);
    }
}
