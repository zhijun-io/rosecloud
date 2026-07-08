package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.common.security.context.UserContext;
import io.rosecloud.starter.security.SecurityErrorCode;
import io.rosecloud.system.domain.SettingKey;
import io.rosecloud.system.domain.SettingKeyRepository;
import io.rosecloud.system.domain.UserSetting;
import io.rosecloud.system.domain.UserSettingRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSettingServiceImplTest {

    @Mock SettingKeyRepository settingKeyRepository;
    @Mock UserSettingRepository userSettingRepository;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private UserSettingServiceImpl service() {
        return new UserSettingServiceImpl(settingKeyRepository, userSettingRepository);
    }

    @Test
    void saveRequiresLoggedInUser() {
        BizException ex = assertThrows(BizException.class,
                () -> service().saveMine("ui.theme", new SettingValueRequest("dark")));

        assertEquals(SecurityErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void saveStoresCurrentUserId() {
        UserContext.set(new CurrentUser(11L, "alice", null, List.of(), List.of()));
        when(settingKeyRepository.findByKey("ui.theme")).thenReturn(Optional.of(
                new SettingKey("ui.theme", "主题", null, LocalDateTime.now(), 1L)));

        service().saveMine("ui.theme", new SettingValueRequest("dark"));

        ArgumentCaptor<UserSetting> captor = ArgumentCaptor.forClass(UserSetting.class);
        verify(userSettingRepository).save(captor.capture());
        assertEquals(11L, captor.getValue().userId());
        assertEquals("ui.theme", captor.getValue().key());
        assertEquals("dark", captor.getValue().value());
        assertEquals(11L, captor.getValue().updatedBy());
    }

    @Test
    void getRejectsMissingSetting() {
        UserContext.set(new CurrentUser(11L, "alice", null, List.of(), List.of()));
        when(userSettingRepository.findByUserIdAndKey(11L, "ui.theme")).thenReturn(Optional.empty());

        BizException ex = assertThrows(BizException.class, () -> service().getMine("ui.theme"));

        assertEquals(SystemErrorCode.USER_SETTING_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void listDelegatesToCurrentUser() {
        UserContext.set(new CurrentUser(11L, "alice", null, List.of(), List.of()));
        UserSetting setting = new UserSetting(11L, "ui.theme", "dark", LocalDateTime.now(), 11L);
        when(userSettingRepository.findByUserId(11L)).thenReturn(List.of(setting));

        assertEquals(List.of(setting), service().listMine());
        verify(userSettingRepository).findByUserId(eq(11L));
    }

    @Test
    void deleteRejectsMissingSetting() {
        UserContext.set(new CurrentUser(11L, "alice", null, List.of(), List.of()));
        when(userSettingRepository.findByUserIdAndKey(11L, "ui.theme")).thenReturn(Optional.empty());

        BizException ex = assertThrows(BizException.class, () -> service().deleteMine("ui.theme"));

        assertEquals(SystemErrorCode.USER_SETTING_NOT_FOUND, ex.getErrorCode());
        verify(userSettingRepository, never()).deleteByUserIdAndKey(any(), any());
    }
}
