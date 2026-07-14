package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.model.UserPrincipal;
import io.rosecloud.system.domain.SystemSetting;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.SettingKeyDao;
import io.rosecloud.system.persistence.SystemSettingDao;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemSettingServiceImplTest {
    @Mock
    SystemSettingDao systemSettingDao;
    @Mock
    SettingKeyDao settingKeyDao;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private SystemSettingServiceImpl service() {
        return new SystemSettingServiceImpl(systemSettingDao, settingKeyDao);
    }

    private static void setCurrentUser(Long userId, String username) {
        SecurityUser su = new SecurityUser(userId, username, null, null, true, null,
                new UserPrincipal(UserPrincipal.Type.USER_NAME, username), List.of());
        var auth = new UsernamePasswordAuthenticationToken(su, null, su.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void saveRejectsUnknownKey() {
        when(settingKeyDao.existsByKey("ui.theme")).thenReturn(false);
        BizException ex = assertThrows(BizException.class,
                () -> service().save("ui.theme", new SettingValueRequest("dark")));
        assertEquals(SystemErrorCode.SETTING_KEY_NOT_FOUND, ex.getErrorCode());
        verify(systemSettingDao, never()).save(any());
    }

    @Test
    void saveStoresValueAndOperator() {
        setCurrentUser(9L, "admin");
        when(settingKeyDao.existsByKey("ui.theme")).thenReturn(true);

        service().save("ui.theme", new SettingValueRequest("{\"mode\":\"dark\"}"));

        ArgumentCaptor<SystemSetting> captor = ArgumentCaptor.forClass(SystemSetting.class);
        verify(systemSettingDao).save(captor.capture());
        assertEquals("ui.theme", captor.getValue().getKey());
        assertEquals("{\"mode\":\"dark\"}", captor.getValue().getValue());
    }

    @Test
    void getRejectsMissingSetting() {
        BizException ex = assertThrows(BizException.class, () -> service().get("ui.theme"));
        assertEquals(SystemErrorCode.SYSTEM_SETTING_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void listDelegatesToMapper() {
        when(systemSettingDao.findAllOrderByKey()).thenReturn(List.of(
                new SystemSetting("ui.theme", "dark", LocalDateTime.now(), 1L)
        ));
        List<SystemSetting> result = service().list();
        assertEquals(1, result.size());
        assertEquals("ui.theme", result.get(0).getKey());
        assertEquals("dark", result.get(0).getValue());
    }

    @Test
    void deleteRejectsMissingSetting() {
        when(settingKeyDao.existsByKey("ui.theme")).thenReturn(true);
        BizException ex = assertThrows(BizException.class, () -> service().delete("ui.theme"));
        assertEquals(SystemErrorCode.SYSTEM_SETTING_NOT_FOUND, ex.getErrorCode());
        verify(systemSettingDao, never()).removeById(any());
    }
}
