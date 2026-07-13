package io.rosecloud.system.service.impl;

import io.rosecloud.api.notice.NoticePublishApi;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.system.config.UserActivationProperties;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.TenantEntity;
import io.rosecloud.system.persistence.TenantMapper;
import io.rosecloud.system.persistence.UserCredentialEntity;
import io.rosecloud.system.persistence.UserCredentialMapper;
import io.rosecloud.system.persistence.UserEntity;
import io.rosecloud.system.persistence.UserMapper;
import io.rosecloud.system.service.dto.UserActivationInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserActivationServiceImplTest {

    @Mock
    UserMapper userMapper;
    @Mock
    UserCredentialMapper userCredentialMapper;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    NoticePublishApi noticePublishApi;
    @Mock
    TenantMapper tenantMapper;

    private UserActivationProperties properties() {
        UserActivationProperties p = new UserActivationProperties();
        p.setActivationTtlHours(72);
        p.setActivationLinkBaseUrl("http://localhost:8080");
        p.setResendCooldownSeconds(60);
        return p;
    }

    private UserActivationServiceImpl service() {
        return new UserActivationServiceImpl(userMapper, userCredentialMapper, passwordEncoder,
                noticePublishApi, tenantMapper, properties());
    }

    @Test
    void confirmActivationTransitionsPendingTenantToEnabledWhenAdminMatches() {
        String token = "activate-token-1";
        String password = "SecurePass1!";
        String encoded = "encoded-hash";
        UserEntity user = userEntity(1L, "admin@tenant1.com", "TENANT1");
        UserCredentialEntity credential = credential(1L, token, false);
        TenantEntity tenant = tenantEntity("TENANT1", "admin@tenant1.com", 0);

        when(userCredentialMapper.selectOne(any())).thenReturn(credential, credential, credential);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(tenantMapper.selectById("TENANT1")).thenReturn(tenant);
        when(passwordEncoder.encode(password)).thenReturn(encoded);
        when(userMapper.selectOne(any())).thenReturn(user);

        UserActivationInfo info = service().confirm(token, password);

        verify(tenantMapper).updateById(tenant);
        assertEquals(Integer.valueOf(1), tenant.getStatus());
    }

    @Test
    void confirmActivationDoesNotTransitionWhenAdminUsernameDoesNotMatch() {
        String token = "activate-token-2";
        String password = "SecurePass2@";
        String encoded = "encoded-hash-2";
        UserEntity user = userEntity(2L, "user@tenant2.com", "TENANT2");
        UserCredentialEntity credential = credential(2L, token, false);
        TenantEntity tenant = tenantEntity("TENANT2", "different-admin", 0);

        when(userCredentialMapper.selectOne(any())).thenReturn(credential, credential, credential);
        when(userMapper.selectById(2L)).thenReturn(user);
        when(tenantMapper.selectById("TENANT2")).thenReturn(tenant);
        when(passwordEncoder.encode(password)).thenReturn(encoded);
        when(userMapper.selectOne(any())).thenReturn(user);

        UserActivationInfo info = service().confirm(token, password);

        verify(tenantMapper, never()).updateById(any(TenantEntity.class));
    }

    @Test
    void confirmActivationDoesNotTransitionWhenTenantAlreadyEnabled() {
        String token = "activate-token-3";
        String password = "SecurePass3#";
        String encoded = "encoded-hash-3";
        UserEntity user = userEntity(3L, "admin@tenant3.com", "TENANT3");
        UserCredentialEntity credential = credential(3L, token, false);
        TenantEntity tenant = tenantEntity("TENANT3", "admin@tenant3.com", 1);

        when(userCredentialMapper.selectOne(any())).thenReturn(credential, credential, credential);
        when(userMapper.selectById(3L)).thenReturn(user);
        when(tenantMapper.selectById("TENANT3")).thenReturn(tenant);
        when(passwordEncoder.encode(password)).thenReturn(encoded);
        when(userMapper.selectOne(any())).thenReturn(user);

        UserActivationInfo info = service().confirm(token, password);

        verify(tenantMapper, never()).updateById(any(TenantEntity.class));
    }

    @Test
    void checkReturnsActivationInfoForValidToken() {
        String token = "valid-token";
        UserCredentialEntity credential = credential(1L, token, false);
        UserEntity user = userEntity(1L, "admin@test.com", "TENANT1");

        when(userCredentialMapper.selectOne(any())).thenReturn(credential);
        when(userMapper.selectById(1L)).thenReturn(user);

        UserActivationInfo info = service().check(token);

        assertEquals("admin@test.com", info.username());
    }

    @Test
    void checkThrowsForInvalidToken() {
        when(userCredentialMapper.selectOne(any())).thenReturn(null);

        assertThrows(BizException.class, () -> service().check("bogus-token"));
    }

    private static UserEntity userEntity(Long id, String email, String tenantId) {
        UserEntity e = new UserEntity();
        e.setId(id);
        e.setEmail(email);
        e.setTenantId(tenantId);
        e.setStatus(0);
        return e;
    }

    private static UserCredentialEntity credential(Long userId, String token, boolean used) {
        UserCredentialEntity e = new UserCredentialEntity();
        e.setUserId(userId);
        e.setActivateToken(token);
        e.setExpireTime(LocalDateTime.now().plusHours(72));
        e.setUsedTime(used ? LocalDateTime.now() : null);
        return e;
    }

    private static TenantEntity tenantEntity(String id, String adminUsername, int status) {
        TenantEntity e = new TenantEntity();
        e.setId(id);
        e.setAdminUsername(adminUsername);
        e.setStatus(status);
        return e;
    }
}
