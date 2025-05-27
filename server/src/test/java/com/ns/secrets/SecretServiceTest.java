package com.ns.secrets;

import com.ns.secrets.repository.EtcdRepository;
import com.ns.secrets.service.PermissionService;
import com.ns.secrets.service.SecretService;
import com.ns.secrets.utils.AesEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class SecretServiceTest {
    private static final String CURRENT_NAMESPACE = "default";
    private static final String USER_ALICE = "alice";
    private static final String KEY_API = "apiKey";

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

        when(permissionService.isAllowed(anyString(), anyString(), anyString())).thenReturn(true); // Permission 허용
    }

    @Test
    void saveEncrypted_shouldThrowException_whenNotAllowed() {
        when(permissionService.isAllowed(eq(USER_ALICE), eq("secret:encrypt"), anyString()))
                .thenReturn(false);

        assertThrows(SecurityException.class, () -> {
            secretService.saveEncrypted(USER_ALICE, CURRENT_NAMESPACE, KEY_API, "secret-value");
        });
    }

    @Test
    void getRaw_shouldThrowException_whenPermissionDenied() {
        String KEY_PASSWORD = "password";

        when(permissionService.isAllowed(eq(USER_ALICE), eq("secret:read"), anyString()))
                .thenReturn(false);

        assertThrows(SecurityException.class, () -> secretService.getRaw(USER_ALICE, CURRENT_NAMESPACE, KEY_PASSWORD));
    }

    @Test
    void saveEncryptedWithTTL_shouldStoreWithLease() {
        String plainText = "my-secret";
        String encrypted = "encrypted-secret";
        long ttl = 60L;
        long leaseId = 12345L;

        when(aesEncryptor.encrypt(plainText)).thenReturn(encrypted);
        when(etcdRepository.grantLease(ttl)).thenReturn(leaseId);

        secretService.saveEncrypted(USER_ALICE, CURRENT_NAMESPACE, KEY_API, plainText, true, ttl);

        verify(etcdRepository).putWithLease("/secrets/" + USER_ALICE + "/" + KEY_API, encrypted, leaseId);
    }

    @Test
    void saveRawWithTTL_shouldStoreRawWithLease() {
        String rawValue = "plaintext";
        String KEY_TOKEN = "token";
        long ttl = 30L;
        long leaseId = 5678L;

        when(etcdRepository.grantLease(ttl)).thenReturn(leaseId);

        secretService.saveRaw(USER_ALICE, CURRENT_NAMESPACE, KEY_TOKEN, rawValue, true, ttl);

        verify(etcdRepository).putWithLease("/secrets/" + USER_ALICE + "/" + KEY_TOKEN, rawValue, leaseId);
    }

}
