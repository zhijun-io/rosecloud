package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.model.UserPrincipal;
import io.rosecloud.system.domain.SettingKey;
import io.rosecloud.system.domain.UserSetting;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.SettingKeyDao;
import io.rosecloud.system.persistence.UserSettingDao;
import io.rosecloud.system.persistence.UserSettingEntity;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSettingServiceImplTest {

    @Mock
    UserSettingDao userSettingDao;
    @Mock
    SettingKeyDao settingKeyDao;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserSettingServiceImpl service() {
        return new UserSettingServiceImpl(userSettingDao, settingKeyDao);
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
        return new SettingKey(11L, "ui.theme", null, null, null, null, null, null);
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
        when(settingKeyDao.findByKey("ui.theme")).thenReturn(Optional.of(knownKey()));
        when(userSettingDao.findEntityByUserIdAndKey(11L, "ui.theme")).thenReturn(null);

        service().saveMine("ui.theme", new SettingValueRequest("dark"));

        ArgumentCaptor<UserSettingEntity> captor = ArgumentCaptor.forClass(UserSettingEntity.class);
        verify(userSettingDao).insert(captor.capture());
        assertEquals(11L, captor.getValue().getUserId());
        assertEquals("ui.theme", captor.getValue().getSettingKey());
        assertEquals("dark", captor.getValue().getValue());
        assertEquals(11L, captor.getValue().getUpdatedBy());
    }

    @Test
    void getRejectsMissingSetting() {
        setCurrentUser(11L, "alice");
        when(userSettingDao.findByUserIdAndKey(11L, "ui.theme")).thenReturn(Optional.empty());

        BizException ex = assertThrows(BizException.class, () -> service().getMine("ui.theme"));

        assertEquals(SystemErrorCode.USER_SETTING_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void listDelegatesToCurrentUser() {
        setCurrentUser(11L, "alice");
        UserSetting setting = new UserSetting(11L, "ui.theme", "dark", LocalDateTime.now(), 11L);
        when(userSettingDao.listByUserId(11L)).thenReturn(List.of(setting));

        List<UserSetting> result = service().listMine();
        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).getUserId());
        assertEquals("ui.theme", result.get(0).getKey());
        assertEquals("dark", result.get(0).getValue());
    }

    @Test
    void deleteRejectsMissingSetting() {
        setCurrentUser(11L, "alice");
        when(userSettingDao.findByUserIdAndKey(11L, "ui.theme")).thenReturn(Optional.empty());

        BizException ex = assertThrows(BizException.class, () -> service().deleteMine("ui.theme"));

        assertEquals(SystemErrorCode.USER_SETTING_NOT_FOUND, ex.getErrorCode());
        verify(userSettingDao, never()).deleteByUserIdAndKey(anyLong(), anyString());
    }
}
