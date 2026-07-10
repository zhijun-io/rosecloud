package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.model.UserPrincipal;
import io.rosecloud.system.domain.SettingKey;
import io.rosecloud.system.domain.UserSetting;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.SettingKeyEntity;
import io.rosecloud.system.persistence.SettingKeyMapper;
import io.rosecloud.system.persistence.UserSettingEntity;
import io.rosecloud.system.persistence.UserSettingMapper;
import io.rosecloud.system.service.dto.SettingValueRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSettingServiceImplTest {

    @Mock
    UserSettingMapper userSettingMapper;
    @Mock
    SettingKeyMapper settingKeyMapper;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserSettingServiceImpl service() {
        return new UserSettingServiceImpl(userSettingMapper, settingKeyMapper);
    }

    private static void setCurrentUser(Long userId, String username) {
        SecurityUser securityUser = new SecurityUser(
                userId, username, null, null, true, null,
                new UserPrincipal(UserPrincipal.Type.USER_NAME, username),
                List.of());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static SettingKeyEntity knownKey() {
        SettingKeyEntity sk = new SettingKeyEntity();
        sk.setId(11L);
        sk.setKey("ui.theme");
        return sk;
    }

    @Test
    void saveRequiresLoggedInUser() {
        BizException ex = assertThrows(BizException.class,
                () -> service().saveMine("ui.theme", new SettingValueRequest("dark")));

        assertEquals(SecurityErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void saveStoresCurrentUserId() {
        setCurrentUser(11L, "alice");
        when(settingKeyMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(knownKey());
        when(userSettingMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service().saveMine("ui.theme", new SettingValueRequest("dark"));

        ArgumentCaptor<UserSettingEntity> captor = ArgumentCaptor.forClass(UserSettingEntity.class);
        verify(userSettingMapper).insert(captor.capture());
        assertEquals(11L, captor.getValue().getUserId());
        assertEquals("ui.theme", captor.getValue().getSettingKey());
        assertEquals("dark", captor.getValue().getValue());
        assertEquals(11L, captor.getValue().getUpdatedBy());
    }

    @Test
    void getRejectsMissingSetting() {
        setCurrentUser(11L, "alice");
        when(userSettingMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> service().getMine("ui.theme"));

        assertEquals(SystemErrorCode.USER_SETTING_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void listDelegatesToCurrentUser() {
        setCurrentUser(11L, "alice");
        UserSettingEntity setting = new UserSettingEntity();
        setting.setUserId(11L);
        setting.setSettingKey("ui.theme");
        setting.setValue("dark");
        setting.setUpdatedAt(LocalDateTime.now());
        setting.setUpdatedBy(11L);
        when(userSettingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(setting));

        List<UserSetting> result = service().listMine();
        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).getUserId());
        assertEquals("ui.theme", result.get(0).getKey());
        assertEquals("dark", result.get(0).getValue());
    }

    @Test
    void deleteRejectsMissingSetting() {
        setCurrentUser(11L, "alice");
        when(userSettingMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> service().deleteMine("ui.theme"));

        assertEquals(SystemErrorCode.USER_SETTING_NOT_FOUND, ex.getErrorCode());
        verify(userSettingMapper, never()).delete(any(LambdaQueryWrapper.class));
    }
}
