package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.model.UserPrincipal;
import io.rosecloud.system.domain.SettingKey;
import io.rosecloud.system.domain.SystemSetting;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.SettingKeyEntity;
import io.rosecloud.system.persistence.SettingKeyMapper;
import io.rosecloud.system.persistence.SystemSettingEntity;
import io.rosecloud.system.persistence.SystemSettingMapper;
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
class SystemSettingServiceImplTest {
    @Mock
    SystemSettingMapper systemSettingMapper;
    @Mock
    SettingKeyMapper settingKeyMapper;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private SystemSettingServiceImpl service() {
        return new SystemSettingServiceImpl(systemSettingMapper, settingKeyMapper);
    }

    private static void setCurrentUser(Long userId, String username) {
        SecurityUser su = new SecurityUser(userId, username, null, null, true, null,
                new UserPrincipal(UserPrincipal.Type.USER_NAME, username), List.of());
        var auth = new UsernamePasswordAuthenticationToken(su, null, su.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static SettingKeyEntity knownKey() {
        SettingKeyEntity sk = new SettingKeyEntity();
        sk.setId(9L);
        sk.setKey("ui.theme");
        return sk;
    }

    @Test
    void saveRejectsUnknownKey() {
        when(settingKeyMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        BizException ex = assertThrows(BizException.class,
                () -> service().save("ui.theme", new SettingValueRequest("dark")));
        assertEquals(SystemErrorCode.SETTING_KEY_NOT_FOUND, ex.getErrorCode());
        verify(systemSettingMapper, never()).insert(any(SystemSettingEntity.class));
        verify(systemSettingMapper, never()).updateById(any(SystemSettingEntity.class));
    }

    @Test
    void saveStoresValueAndOperator() {
        setCurrentUser(9L, "admin");
        when(settingKeyMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(knownKey());
        when(systemSettingMapper.selectById(anyString())).thenReturn(null);
        service().save("ui.theme", new SettingValueRequest("{\"mode\":\"dark\"}"));
        ArgumentCaptor<SystemSettingEntity> captor = ArgumentCaptor.forClass(SystemSettingEntity.class);
        verify(systemSettingMapper).insert(captor.capture());
        assertEquals("ui.theme", captor.getValue().getSettingKey());
        assertEquals("{\"mode\":\"dark\"}", captor.getValue().getValue());
        assertEquals(9L, captor.getValue().getUpdatedBy());
    }

    @Test
    void getRejectsMissingSetting() {
        when(systemSettingMapper.selectById(anyString())).thenReturn(null);
        BizException ex = assertThrows(BizException.class, () -> service().get("ui.theme"));
        assertEquals(SystemErrorCode.SYSTEM_SETTING_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void listDelegatesToMapper() {
        SystemSettingEntity setting = new SystemSettingEntity();
        setting.setSettingKey("ui.theme");
        setting.setValue("dark");
        setting.setUpdatedAt(LocalDateTime.now());
        setting.setUpdatedBy(1L);
        when(systemSettingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(setting));
        List<SystemSetting> result = service().list();
        assertEquals(1, result.size());
        assertEquals("ui.theme", result.get(0).getKey());
        assertEquals("dark", result.get(0).getValue());
    }

    @Test
    void deleteRejectsMissingSetting() {
        when(settingKeyMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(knownKey());
        when(systemSettingMapper.selectById(anyString())).thenReturn(null);
        BizException ex = assertThrows(BizException.class, () -> service().delete("ui.theme"));
        assertEquals(SystemErrorCode.SYSTEM_SETTING_NOT_FOUND, ex.getErrorCode());
        verify(systemSettingMapper, never()).deleteById(anyString());
    }
}
