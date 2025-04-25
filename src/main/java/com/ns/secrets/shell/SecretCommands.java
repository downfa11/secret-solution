package com.ns.secrets.shell;

import com.ns.secrets.service.SecretService;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.util.List;

@ShellComponent
@RequiredArgsConstructor
public class SecretCommands {

    private final SecretService secretService;

    @ShellMethod(key = "save-encrypted", value = "Encrypts and saves the secret")
    public String saveEncrypted(String userId, String key, String plainText) {
        secretService.saveEncrypted(userId, key, plainText);
        return "Encrypted secret saved for user " + userId + " with key " + key;
    }

    @ShellMethod(key = "get-decrypted", value = "Fetches and decrypts the secret")
    public String getDecrypted(String userId, String key) {
        String decrypted = secretService.getDecrypted(userId, key);
        if (decrypted == null) {
            return "No secret found for user " + userId + " with key " + key;
        }
        return "Decrypted secret for user " + userId + " with key " + key + ": " + decrypted;
    }

    @ShellMethod(key = "save-raw", value = "Saves the secret without encryption")
    public String saveRaw(String userId, String key, String value) {
        secretService.saveRaw(userId, key, value);
        return "Raw secret saved for user " + userId + " with key " + key;
    }

    @ShellMethod(key = "get-raw", value = "Fetches the raw secret")
    public String getRaw(String userId, String key) {
        String raw = secretService.getRaw(userId, key);
        if (raw == null) {
            return "No raw secret found for user " + userId + " with key " + key;
        }
        return "Raw secret for user " + userId + " with key " + key + ": " + raw;
    }

    @ShellMethod(key = "save-with-ttl", value = "Encrypts and saves the secret with TTL")
    public String saveWithTTL(String userId, String key, String plainText, long ttlSeconds) {
        secretService.saveWithTTL(userId, key, plainText, ttlSeconds);
        return "Encrypted secret with TTL saved for user " + userId + " with key " + key + " for " + ttlSeconds + " seconds";
    }

    @ShellMethod(key = "list-secrets", value = "Lists all stored secrets")
    public String listSecrets() {
        List<String> allSecrets = secretService.getAllSecrets();
        if (allSecrets.isEmpty()) {
            return "No secrets found.";
        }
        return String.join("\n", allSecrets);
    }
}
