package io.rosecloud.auth.service.impl;

import io.rosecloud.auth.persistence.AuthCredentialMapper;
import io.rosecloud.auth.persistence.CredentialEntity;
import io.rosecloud.auth.service.CredentialService;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.security.credential.AuthCredential;
import io.rosecloud.common.security.credential.CredentialsChangedEvent;
import io.rosecloud.common.security.credential.PasswordPolicyValidator;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import io.rosecloud.auth.service.LoginSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialServiceTest {

    @Mock
    AuthCredentialMapper credentialMapper;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    LoginSessionService loginSessionService;
    @Mock
    PasswordPolicyValidator passwordPolicyValidator;
    @Mock
    ApplicationEventPublisher eventPublisher;

    private CredentialService service;

    @BeforeEach
    void setUp() {
        service = new CredentialService(credentialMapper, passwordEncoder,
                loginSessionService, passwordPolicyValidator, eventPublisher);
    }

    // ---- findByUserId ----

    @Test
    void findByUserIdReturnsCredentialWhenExists() {
        CredentialEntity entity = new CredentialEntity();
        entity.setUserId(1L);
        entity.setPasswordHash("hash");
        entity.setPasswordChangedTime(LocalDateTime.now());
        entity.setAuthStatus(1);
        when(credentialMapper.selectOne(any())).thenReturn(entity);

        Optional<AuthCredential> result = service.findByUserId(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().userId());
        assertTrue(result.get().enabled());
    }

    @Test
    void findByUserIdReturnsEmptyWhenNotFound() {
        when(credentialMapper.selectOne(any())).thenReturn(null);

        Optional<AuthCredential> result = service.findByUserId(1L);

        assertFalse(result.isPresent());
    }

    // ---- setPassword ----

    @Test
    void setPasswordInsertsNewCredential() {
        when(credentialMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("newPass")).thenReturn("encoded");

        service.setPassword(1L, "newPass");

        verify(passwordPolicyValidator).validate("newPass");
        verify(credentialMapper).insert(any(CredentialEntity.class));
        verify(eventPublisher).publishEvent(any(CredentialsChangedEvent.class));
    }

    @Test
    void setPasswordUpdatesExistingCredential() {
        CredentialEntity existing = new CredentialEntity();
        existing.setUserId(1L);
        existing.setPasswordHash("oldHash");
        existing.setAuthStatus(0);
        when(credentialMapper.selectOne(any())).thenReturn(existing);
        when(passwordEncoder.encode("newPass")).thenReturn("encoded");

        service.setPassword(1L, "newPass");

        verify(passwordPolicyValidator).validate("newPass");
        verify(credentialMapper).updateById(any(CredentialEntity.class));
        verify(eventPublisher).publishEvent(any(CredentialsChangedEvent.class));
    }

    @Test
    void setPasswordFailsOnWeakPassword() {
        doThrow(new BizException(SecurityErrorCode.PASSWORD_TOO_WEAK))
                .when(passwordPolicyValidator).validate("weak");

        assertThrows(BizException.class, () -> service.setPassword(1L, "weak"));
        verifyNoInteractions(passwordEncoder, eventPublisher);
    }

    // ---- changePassword ----

    @Test
    void changePasswordFailsOnWrongCurrentPassword() {
        when(credentialMapper.selectOne(any())).thenReturn(entityWithHash("correctHash"));
        when(passwordEncoder.matches("wrongPass", "correctHash")).thenReturn(false);

        BizException ex = assertThrows(BizException.class,
                () -> service.changePassword(1L, "wrongPass", "newPass"));

        assertEquals(SecurityErrorCode.BAD_CREDENTIALS, ex.getErrorCode());
        verifyNoInteractions(passwordPolicyValidator);
    }

    @Test
    void changePasswordFailsOnSamePassword() {
        when(credentialMapper.selectOne(any())).thenReturn(entityWithHash("hash"));
        when(passwordEncoder.matches("samePass", "hash")).thenReturn(true);
        doThrow(new BizException(SecurityErrorCode.PASSWORD_SAME_AS_OLD))
                .when(passwordPolicyValidator).validateChange("samePass", "samePass");

        BizException ex = assertThrows(BizException.class,
                () -> service.changePassword(1L, "samePass", "samePass"));

        assertEquals(SecurityErrorCode.PASSWORD_SAME_AS_OLD, ex.getErrorCode());
    }

    @Test
    void changePasswordSucceedsAndPublishesEvent() {
        CredentialEntity existing = entityWithHash("oldHash");
        existing.setAuthStatus(1);
        when(credentialMapper.selectOne(any())).thenReturn(existing);
        when(passwordEncoder.matches("oldPass", "oldHash")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("encoded");

        service.changePassword(1L, "oldPass", "newPass");

        verify(passwordPolicyValidator).validateChange("oldPass", "newPass");
        verify(passwordPolicyValidator).validate("newPass");
        verify(credentialMapper).updateById(any(CredentialEntity.class));
        verify(eventPublisher).publishEvent(any(CredentialsChangedEvent.class));
    }

    @Test
    void changePasswordPublishesEventNotDirectRevoke() {
        CredentialEntity existing = entityWithHash("hash");
        existing.setAuthStatus(1);
        when(credentialMapper.selectOne(any())).thenReturn(existing);
        when(passwordEncoder.matches("oldPass", "hash")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("encoded");

        service.changePassword(1L, "oldPass", "newPass");

        // Session revocation is now driven by the event listener, not directly here
        verify(eventPublisher).publishEvent(any(CredentialsChangedEvent.class));
        verifyNoInteractions(loginSessionService);
    }

    private static CredentialEntity entityWithHash(String hash) {
        CredentialEntity e = new CredentialEntity();
        e.setUserId(1L);
        e.setPasswordHash(hash);
        e.setPasswordChangedTime(LocalDateTime.now());
        return e;
    }
}
