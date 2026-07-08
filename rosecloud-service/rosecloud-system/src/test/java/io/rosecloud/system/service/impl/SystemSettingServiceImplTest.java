package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.common.security.context.UserContext;
import io.rosecloud.system.domain.SettingKey;
import io.rosecloud.system.domain.SettingKeyRepository;
import io.rosecloud.system.domain.SystemSetting;
import io.rosecloud.system.domain.SystemSettingRepository;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.dto.SettingValueRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Mock SettingKeyRepository settingKeyRepository;
    @Mock SystemSettingRepository systemSettingRepository;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private SystemSettingServiceImpl service() {
        return new SystemSettingServiceImpl(settingKeyRepository, systemSettingRepository);
    }

    @Test
    void saveRejectsUnknownKey() {
        when(settingKeyRepository.findByKey("ui.theme")).thenReturn(Optional.empty());

        BizException ex = assertThrows(BizException.class,
                () -> service().save("ui.theme", new SettingValueRequest("dark")));

        assertEquals(SystemErrorCode.SETTING_KEY_NOT_FOUND, ex.getErrorCode());
        verify(systemSettingRepository, never()).save(any());
    }

    @Test
    void saveStoresValueAndOperator() {
        UserContext.set(new CurrentUser(9L, "admin", null, List.of(), List.of()));
        when(settingKeyRepository.findByKey("ui.theme")).thenReturn(Optional.of(
                new SettingKey("ui.theme", "主题", null, LocalDateTime.now(), 1L)));

        service().save("ui.theme", new SettingValueRequest("{\"mode\":\"dark\"}"));

        ArgumentCaptor<SystemSetting> captor = ArgumentCaptor.forClass(SystemSetting.class);
        verify(systemSettingRepository).save(captor.capture());
        assertEquals("ui.theme", captor.getValue().getKey());
        assertEquals("{\"mode\":\"dark\"}", captor.getValue().getValue());
        assertEquals(9L, captor.getValue().getUpdatedBy());
    }

    @Test
    void getRejectsMissingSetting() {
        when(systemSettingRepository.findByKey("ui.theme")).thenReturn(Optional.empty());

        BizException ex = assertThrows(BizException.class, () -> service().get("ui.theme"));

        assertEquals(SystemErrorCode.SYSTEM_SETTING_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void listDelegatesToRepository() {
        SystemSetting setting = new SystemSetting("ui.theme", "dark", LocalDateTime.now(), 1L);
        when(systemSettingRepository.findAll()).thenReturn(List.of(setting));

        assertEquals(List.of(setting), service().list());
    }

    @Test
    void deleteRejectsMissingSetting() {
        when(settingKeyRepository.findByKey("ui.theme")).thenReturn(Optional.of(
                new SettingKey("ui.theme", "主题", null, LocalDateTime.now(), 1L)));
        when(systemSettingRepository.findByKey("ui.theme")).thenReturn(Optional.empty());

        BizException ex = assertThrows(BizException.class, () -> service().delete("ui.theme"));

        assertEquals(SystemErrorCode.SYSTEM_SETTING_NOT_FOUND, ex.getErrorCode());
        verify(systemSettingRepository, never()).deleteByKey("ui.theme");
    }
}
