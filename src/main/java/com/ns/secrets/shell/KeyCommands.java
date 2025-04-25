package com.ns.secrets.shell;

import com.ns.secrets.utils.AesEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.security.SecureRandom;
import java.util.Base64;

@ShellComponent
@RequiredArgsConstructor
public class KeyCommands {
    private final AesEncryptor aesEncryptor;

    @ShellMethod(key = "generate-aes-key", value = "Generates a new key")
    public String generateAesKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    @ShellMethod(key = "set-aes-key", value = "Sets a new AES key")
    public String setAesKey(String newKey) {
        try {
            aesEncryptor.setKey(newKey);
            return "AES key updated successfully.";
        } catch (Exception e) {
            return "Failed to update AES key: " + e.getMessage();
        }
    }
}
