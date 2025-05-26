package com.ns.secrets;

import com.ns.secrets.repository.EtcdRepository;
import com.ns.secrets.service.PermissionService;
import com.ns.secrets.service.SecretService;
import com.ns.secrets.utils.AesEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class SecretServiceTest {
    private static final String USER_ALICE = "alice";
    private static final String USER_BOB = "bob";
    private static final String KEY_API = "apiKey";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_TOKEN = "token";

    private EtcdRepository etcdRepository;
    private AesEncryptor aesEncryptor;
    private SecretService secretService;
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        etcdRepository = mock(EtcdRepository.class);
        aesEncryptor = mock(AesEncryptor.class);
        permissionService = mock(PermissionService.class);
        secretService = new SecretService(etcdRepository, aesEncryptor, permissionService);
    }

    @Test
    void saveEncrypted_shouldEncryptAndStoreToEtcd() {
        String plainText = "my-secret";
        String encrypted = "encrypted123";

        when(aesEncryptor.encrypt(plainText)).thenReturn(encrypted);

        secretService.saveEncrypted(USER_ALICE, KEY_API, plainText);

        verify(aesEncryptor).encrypt(plainText);
        verify(etcdRepository).put("/secrets/" + USER_ALICE + "/" + KEY_API, encrypted);
    }

    @Test
    void getDecrypted_shouldFetchFromEtcdAndDecrypt() {
        String encrypted = "enc-xyz";
        String decrypted = "superpass";

        when(etcdRepository.get("/secrets/" + USER_BOB + "/" + KEY_PASSWORD)).thenReturn(encrypted);
        when(aesEncryptor.decrypt(encrypted)).thenReturn(decrypted);

        String result = secretService.getDecrypted(USER_BOB, KEY_PASSWORD);

        assertEquals(decrypted, result);
        verify(etcdRepository).get("/secrets/" + USER_BOB + "/" + KEY_PASSWORD);
        verify(aesEncryptor).decrypt(encrypted);
    }

    @Test
    void getDecrypted_shouldReturnNull_whenKeyNotFound() {
        when(etcdRepository.get("/secrets/" + USER_BOB + "/" + "unknown")).thenReturn(null);

        String result = secretService.getDecrypted(USER_BOB, "unknown");

        assertNull(result);
        verify(aesEncryptor, never()).decrypt(anyString());
    }

    @Test
    void saveRaw_shouldStoreValueWithoutEncryption() {
        String rawValue = "abc123";

        secretService.saveRaw(USER_ALICE, KEY_TOKEN, rawValue);

        verify(etcdRepository).put("/secrets/" + USER_ALICE + "/" + KEY_TOKEN, rawValue);
    }

    @Test
    void getRaw_shouldReturnValueWithoutDecryption() {
        String rawValue = "abc123";
        when(etcdRepository.get("/secrets/" + USER_ALICE + "/" + KEY_TOKEN)).thenReturn(rawValue);

        String result = secretService.getRaw(USER_ALICE, KEY_TOKEN);

        assertEquals(rawValue, result);
    }
}
