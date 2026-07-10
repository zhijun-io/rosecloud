package io.rosecloud.system.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.model.UserPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import io.rosecloud.system.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTest {

    @Mock
    UserMapper userMapper;
    @Mock
    UserCredentialMapper userCredentialMapper;
    @Mock
    UserRoleMapper userRoleMapper;
    @Mock
    RoleMapper roleMapper;
    @Mock
    RoleMenuMapper roleMenuMapper;
    @Mock
    MenuMapper menuMapper;
    @Mock
    UserTenantMapper userTenantMapper;

    private UserRepositoryImpl repository() {
        return new UserRepositoryImpl(userMapper, userCredentialMapper, userRoleMapper, userTenantMapper,
                roleMapper, roleMenuMapper, menuMapper, new ObjectMapper());
    }

    @Test
    void insertStoresPasswordInUserCredential() {
        doAnswer(invocation -> {
            UserEntity po = invocation.getArgument(0);
            po.setId(101L);
            return 1;
        }).when(userMapper).insert(any(UserEntity.class));

        doAnswer(invocation -> 1).when(userCredentialMapper).insert(any(UserCredentialEntity.class));

        User user = new User(null, "alice@example.com", "Alice", 1, "TENANT1", null);

        assertEquals(101L, repository().insert(user, "hash-123"));

        ArgumentCaptor<UserCredentialEntity> credentialCaptor = ArgumentCaptor.forClass(UserCredentialEntity.class);
        verify(userCredentialMapper).insert(credentialCaptor.capture());
        assertEquals(101L, credentialCaptor.getValue().getUserId());
        assertEquals("hash-123", credentialCaptor.getValue().getPassword());
        assertThat(credentialCaptor.getValue().getActivateToken()).isNull();
        assertThat(credentialCaptor.getValue().getPasswordChangedTime()).isNotNull();
    }

    @Test
    void loadByUsernameReadsPasswordFromUserCredential() {
        UserEntity user = new UserEntity();
        user.setId(201L);
        user.setEmail("alice@example.com");
        user.setStatus(1);
        user.setTenantId("TENANT1");
        UserCredentialEntity credential = new UserCredentialEntity();
        credential.setUserId(201L);
        credential.setPassword("hash-456");

        when(userMapper.selectOne(any())).thenReturn(user);
        when(userCredentialMapper.selectOne(any())).thenReturn(credential);
        when(userRoleMapper.selectList(any())).thenReturn(List.of());

        Optional<SecurityUser> authInfo = repository().loadByUsername("alice@example.com");

        assertEquals("hash-456", authInfo.orElseThrow().getPassword());
    }

    @Test
    void updateLastLoginTimeStoresTimestampInUserCredential() {
        UserCredentialEntity credential = new UserCredentialEntity();
        credential.setUserId(301L);

        when(userCredentialMapper.selectOne(any())).thenReturn(credential);
        doAnswer(invocation -> 1).when(userCredentialMapper).updateById(any(UserCredentialEntity.class));

        LocalDateTime loginTime = LocalDateTime.of(2026, 7, 8, 16, 30);
        repository().updateLastLoginTime(301L, loginTime);

        ArgumentCaptor<UserCredentialEntity> credentialCaptor = ArgumentCaptor.forClass(UserCredentialEntity.class);
        verify(userCredentialMapper).updateById(credentialCaptor.capture());
        assertEquals(loginTime, credentialCaptor.getValue().getLastLoginTime());
    }

    @Test
    void updatePasswordStoresPasswordAndChangeTime() {
        UserCredentialEntity credential = new UserCredentialEntity();
        credential.setUserId(302L);

        when(userCredentialMapper.selectOne(any())).thenReturn(credential);
        doAnswer(invocation -> 1).when(userCredentialMapper).updateById(any(UserCredentialEntity.class));

        LocalDateTime changedTime = LocalDateTime.of(2026, 7, 8, 16, 55);
        repository().updatePassword(302L, "new-hash", changedTime);

        ArgumentCaptor<UserCredentialEntity> credentialCaptor = ArgumentCaptor.forClass(UserCredentialEntity.class);
        verify(userCredentialMapper).updateById(credentialCaptor.capture());
        assertEquals("new-hash", credentialCaptor.getValue().getPassword());
        assertEquals(changedTime, credentialCaptor.getValue().getPasswordChangedTime());
    }

    @Test
    void confirmActivationUpdatesPasswordChangeTime() {
        UserCredentialEntity credential = new UserCredentialEntity();
        credential.setUserId(401L);

        when(userCredentialMapper.selectOne(any())).thenReturn(credential);
        doAnswer(invocation -> 1).when(userCredentialMapper).updateById(any(UserCredentialEntity.class));

        LocalDateTime usedTime = LocalDateTime.of(2026, 7, 8, 16, 45);
        repository().confirmActivation(401L, "encoded", usedTime);

        ArgumentCaptor<UserCredentialEntity> credentialCaptor = ArgumentCaptor.forClass(UserCredentialEntity.class);
        verify(userCredentialMapper).updateById(credentialCaptor.capture());
        assertEquals("encoded", credentialCaptor.getValue().getPassword());
        assertEquals(usedTime, credentialCaptor.getValue().getPasswordChangedTime());
        assertEquals(usedTime, credentialCaptor.getValue().getUsedTime());
    }
}
