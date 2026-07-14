package io.rosecloud.system.service.impl;

import io.rosecloud.api.credential.CredentialApi;
import io.rosecloud.api.credential.CredentialSetRequest;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.system.config.UserActivationProperties;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.domain.User;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.TenantDao;
import io.rosecloud.system.persistence.UserCredentialDao;
import io.rosecloud.system.persistence.UserCredentialEntity;
import io.rosecloud.system.persistence.UserDao;
import io.rosecloud.system.service.dto.UserActivationInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserActivationServiceImplTest {

    @Mock
    UserDao userDao;
    @Mock
    UserCredentialDao userCredentialDao;
    @Mock
    TenantDao tenantDao;
    @Mock
    CredentialApi credentialApi;

    private UserActivationProperties properties() {
        UserActivationProperties p = new UserActivationProperties();
        p.setActivationTtlHours(72);
        p.setActivationLinkBaseUrl("http://localhost:8080");
        p.setResendCooldownSeconds(60);
        return p;
    }

    private UserActivationServiceImpl service() {
        return new UserActivationServiceImpl(userDao, userCredentialDao, tenantDao,
                credentialApi, properties());
    }

    @Test
    void confirmActivationTransitionsPendingTenantToEnabledWhenAdminMatches() {
        String token = "activate-token-1";
        String password = "SecurePass1!";
        User user = new User(1L, "admin@tenant1.com", null, 0, "TENANT1", null);
        UserCredentialEntity credential = credential(1L, token, false);
        Tenant tenant = new Tenant("TENANT1", null, TenantStatus.PENDING, "admin@tenant1.com", null, null, null, null, null);

        when(userCredentialDao.findByActivateToken(token)).thenReturn(Optional.of(credential));
        when(userDao.findById(1L)).thenReturn(Optional.of(user));
        when(userCredentialDao.findByUserId(1L)).thenReturn(Optional.of(credential));
        when(tenantDao.findById("TENANT1")).thenReturn(Optional.of(tenant));
        when(tenantDao.isAdminUser(anyString(), anyString())).thenReturn(true);
        when(userDao.findByEmailOrPhone("admin@tenant1.com")).thenReturn(Optional.of(user));
        when(userDao.save(any())).thenReturn(user);

        UserActivationInfo info = service().confirm(token, password);

        verify(credentialApi).setPassword(1L, new CredentialSetRequest(password));
        verify(tenantDao).updateStatus("TENANT1", 1);
    }

    @Test
    void confirmActivationDoesNotTransitionWhenAdminUsernameDoesNotMatch() {
        String token = "activate-token-2";
        String password = "SecurePass2@";
        User user = new User(2L, "user@tenant2.com", null, 0, "TENANT2", null);
        UserCredentialEntity credential = credential(2L, token, false);
        Tenant tenant = new Tenant("TENANT2", null, TenantStatus.PENDING, "different-admin", null, null, null, null, null);

        when(userCredentialDao.findByActivateToken(token)).thenReturn(Optional.of(credential));
        when(userDao.findById(2L)).thenReturn(Optional.of(user));
        when(userCredentialDao.findByUserId(2L)).thenReturn(Optional.of(credential));
        when(tenantDao.findById("TENANT2")).thenReturn(Optional.of(tenant));
        when(tenantDao.isAdminUser(anyString(), anyString())).thenReturn(false);
        when(userDao.findByEmailOrPhone("user@tenant2.com")).thenReturn(Optional.of(user));
        when(userDao.save(any())).thenReturn(user);

        UserActivationInfo info = service().confirm(token, password);

        verify(credentialApi).setPassword(2L, new CredentialSetRequest(password));
        verify(tenantDao, never()).updateStatus(anyString(), anyInt());
    }

    @Test
    void confirmActivationDoesNotTransitionWhenTenantAlreadyEnabled() {
        String token = "activate-token-3";
        String password = "SecurePass3#";
        User user = new User(3L, "admin@tenant3.com", null, 0, "TENANT3", null);
        UserCredentialEntity credential = credential(3L, token, false);
        Tenant tenant = new Tenant("TENANT3", null, TenantStatus.ENABLED, "admin@tenant3.com", null, null, null, null, null);

        when(userCredentialDao.findByActivateToken(token)).thenReturn(Optional.of(credential));
        when(userDao.findById(3L)).thenReturn(Optional.of(user));
        when(userCredentialDao.findByUserId(3L)).thenReturn(Optional.of(credential));
        when(tenantDao.findById("TENANT3")).thenReturn(Optional.of(tenant));
        when(tenantDao.isAdminUser(anyString(), anyString())).thenReturn(true);
        when(userDao.findByEmailOrPhone("admin@tenant3.com")).thenReturn(Optional.of(user));
        when(userDao.save(any())).thenReturn(user);

        UserActivationInfo info = service().confirm(token, password);

        verify(credentialApi).setPassword(3L, new CredentialSetRequest(password));
        verify(tenantDao, never()).updateStatus(anyString(), anyInt());
    }

    @Test
    void checkReturnsActivationInfoForValidToken() {
        String token = "valid-token";
        UserCredentialEntity credential = credential(1L, token, false);
        User user = new User(1L, "admin@test.com", null, 0, "TENANT1", null);

        when(userCredentialDao.findByActivateToken(token)).thenReturn(Optional.of(credential));
        when(userDao.findById(1L)).thenReturn(Optional.of(user));

        UserActivationInfo info = service().check(token);

        assertEquals("admin@test.com", info.username());
    }

    @Test
    void checkThrowsForInvalidToken() {
        when(userCredentialDao.findByActivateToken("bogus-token")).thenReturn(Optional.empty());

        assertThrows(BizException.class, () -> service().check("bogus-token"));
    }

    private static UserCredentialEntity credential(Long userId, String token, boolean used) {
        UserCredentialEntity e = new UserCredentialEntity();
        e.setUserId(userId);
        e.setActivateToken(token);
        e.setExpireTime(LocalDateTime.now().plusHours(72));
        e.setUsedTime(used ? LocalDateTime.now() : null);
        return e;
    }
}
