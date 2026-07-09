package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.model.UserPrincipal;
import io.rosecloud.system.domain.SettingKey;
import io.rosecloud.system.domain.SettingKeyRepository;
import io.rosecloud.system.domain.SystemSettingRepository;
import io.rosecloud.system.domain.UserSettingRepository;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.dto.SettingKeyCreateRequest;
import io.rosecloud.system.service.dto.SettingKeyUpdateRequest;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingKeyServiceImplTest {
    @Mock
    SettingKeyRepository settingKeyRepository;
    @Mock
    SystemSettingRepository systemSettingRepository;
    @Mock
    UserSettingRepository userSettingRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private SettingKeyServiceImpl service() {
        return new SettingKeyServiceImpl(settingKeyRepository, systemSettingRepository, userSettingRepository);
    }

    private static void setCurrentUser(Long userId, String username) {
        SecurityUser su = new SecurityUser(userId, username, null, null, true,
                new UserPrincipal(UserPrincipal.Type.USER_NAME, username), List.of());
        var auth = new UsernamePasswordAuthenticationToken(su, null, su.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createRejectsDuplicateKey() {
        when(settingKeyRepository.existsByKey("ui.theme")).thenReturn(true);
        BizException ex = assertThrows(BizException.class,
                () -> service().create(new SettingKeyCreateRequest("ui.theme", "主题", "desc")));
        assertEquals(SystemErrorCode.SETTING_KEY_EXISTS, ex.getErrorCode());
        verify(settingKeyRepository, never()).insert(any());
    }

    @Test
    void createStoresMetadataFromCurrentUser() {
        setCurrentUser(7L, "alice");
        when(settingKeyRepository.existsByKey("ui.theme")).thenReturn(false);
        service().create(new SettingKeyCreateRequest("ui.theme", "主题", "desc"));
        ArgumentCaptor<SettingKey> captor = ArgumentCaptor.forClass(SettingKey.class);
        verify(settingKeyRepository).insert(captor.capture());
        assertEquals("ui.theme", captor.getValue().getKey());
        assertEquals("主题", captor.getValue().getName());
        assertEquals("desc", captor.getValue().getRemark());
    }

    @Test
    void updateRejectsMissingKey() {
        when(settingKeyRepository.findByKey("ui.theme")).thenReturn(Optional.empty());
        BizException ex = assertThrows(BizException.class,
                () -> service().update("ui.theme", new SettingKeyUpdateRequest("主题", "desc")));
        assertEquals(SystemErrorCode.SETTING_KEY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void deleteCascadesToSystemAndUserSettings() {
        when(settingKeyRepository.findByKey("ui.theme")).thenReturn(Optional.of(
                new SettingKey(1L, "ui.theme", "主题", "desc")));
        service().delete("ui.theme");
        verify(systemSettingRepository).deleteByKey("ui.theme");
        verify(userSettingRepository).deleteByKey("ui.theme");
        verify(settingKeyRepository).deleteByKey("ui.theme");
    }

    @Test
    void pageDelegatesToRepository() {
        PageResult<SettingKey> result = PageResult.of(List.of(), 0, 1, 10);
        when(settingKeyRepository.page(1, 10, "ui")).thenReturn(result);
        assertEquals(result, service().page(1, 10, "ui"));
        verify(settingKeyRepository).page(eq(1L), eq(10L), eq("ui"));
    }
}
