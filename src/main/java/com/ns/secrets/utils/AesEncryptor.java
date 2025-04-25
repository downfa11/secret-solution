package com.ns.secrets.utils;


import com.ns.secrets.repository.EtcdRepository;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesEncryptor {
    private static final String AES_KEY_PREFIX = "aesKey";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SecretKeySpec keySpec;
    private final EtcdRepository etcdRepository;


    public AesEncryptor(EtcdRepository etcdRepository) {
        this.etcdRepository = etcdRepository;
        String storedKey = etcdRepository.get(AES_KEY_PREFIX);

        if (storedKey == null) {
            byte[] keyBytes = new byte[32]; // 256-bit AES
            new SecureRandom().nextBytes(keyBytes);
            String encodedKey = Base64.getEncoder().encodeToString(keyBytes);
            etcdRepository.put(AES_KEY_PREFIX, encodedKey);
            storedKey = encodedKey;

            System.out.println("Generated new AES key and stored it in etcd.");
        }

        byte[] keyBytes = Base64.getDecoder().decode(storedKey);
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    public void setKey(String encodedKey) {
        byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
        etcdRepository.put(AES_KEY_PREFIX, encodedKey);
        System.out.println("AES key updated in etcd.");
    }

    public String encrypt(String plain) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            SECURE_RANDOM.nextBytes(iv);

            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);

            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String cipherText) {
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[12];
            byteBuffer.get(iv);

            byte[] cipherBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherBytes);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);

            byte[] decrypted = cipher.doFinal(cipherBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

}