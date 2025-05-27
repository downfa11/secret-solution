package com.ns.secrets.controller;


import com.ns.secrets.service.SecretService;
import com.ns.secrets.utils.AesEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/secrets")
@RequiredArgsConstructor
public class SecretController {
    private final AesEncryptor aesEncryptor;
    private final SecretService secretService;

    @GetMapping("/generate-key")
    public ResponseEntity<String> generateAesKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return ResponseEntity.ok(Base64.getEncoder().encodeToString(key));
    }

    @PostMapping("/set-key")
    public ResponseEntity<String> setAesKey(@RequestBody String newKey) {
        try {
            aesEncryptor.setKey(newKey);
            return ResponseEntity.ok("AES key updated successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update AES key: " + e.getMessage());
        }
    }

    @PostMapping("/put")
    public ResponseEntity<String> saveRaw(
            @RequestParam String executorUserId,
            @RequestParam String namespace,
            @RequestParam String key,
            @RequestParam String plainText
    ) {
        secretService.saveRaw(executorUserId, namespace, key, plainText);
        return ResponseEntity.ok("Raw secret saved in namespace " + namespace + " with key " + key);
    }

    @PostMapping("/put-ttl")
    public ResponseEntity<String> saveRawWithTTL(
            @RequestParam String executorUserId,
            @RequestParam String namespace,
            @RequestParam String key,
            @RequestParam String plainText,
            @RequestParam long ttlSeconds
    ) {
        secretService.saveRaw(executorUserId, namespace, key, plainText, true, ttlSeconds);
        return ResponseEntity.ok("Raw secret saved in namespace " + namespace + " with key " + key);
    }

    @PostMapping("/encrypted")
    public ResponseEntity<String> saveEncrypted(
            @RequestParam String executorUserId,
            @RequestParam String namespace,
            @RequestParam String key,
            @RequestParam String plainText
    ) {
        secretService.saveEncrypted(executorUserId, namespace, key, plainText);
        return ResponseEntity.ok("Encrypted secret saved in namespace " + namespace + " with key " + key);
    }

    @PostMapping("/encrypted-ttl")
    public ResponseEntity<String> saveEncryptedWithTTL(
            @RequestParam String executorUserId,
            @RequestParam String namespace,
            @RequestParam String key,
            @RequestParam String plainText,
            @RequestParam long ttlSeconds
    ) {
        secretService.saveEncrypted(executorUserId, namespace, key, plainText, true, ttlSeconds);
        return ResponseEntity.ok("Encrypted secret with TTL saved in namespace " + namespace + " with key " + key + ", TTL: " + ttlSeconds + "s");
    }

    @GetMapping("/decrypted")
    public ResponseEntity<String> getDecrypted(
            @RequestParam String userId,
            @RequestParam String namespace,
            @RequestParam String key
    ) {
        String decrypted = secretService.getDecrypted(userId, namespace, key);
        if (decrypted == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok("Decrypted secret for user " + userId + " with key " + key + ": " + decrypted);
    }

    @GetMapping("/raw")
    public ResponseEntity<String> getRaw(
            @RequestParam String userId,
            @RequestParam String namespace,
            @RequestParam String key
    ) {
        String raw = secretService.getRaw(userId, namespace, key);
        if (raw == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok("Raw secret for user " + userId + " with key " + key + ": " + raw);
    }

    @GetMapping("/list")
    public ResponseEntity<String> listSecrets(
            @RequestParam String requesterUserId,
            @RequestParam String namespace
    ) {
        List<String> allSecrets = secretService.getAllSecrets(requesterUserId, namespace);
        if (allSecrets.isEmpty()) {
            return ResponseEntity.ok("No secrets found.");
        }
        return ResponseEntity.ok(String.join("\n", allSecrets));
    }
}

