package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.model.UserPrincipal;
import io.rosecloud.system.domain.SettingKey;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.SettingKeyEntity;
import io.rosecloud.system.persistence.SettingKeyMapper;
import io.rosecloud.system.persistence.SystemSettingEntity;
import io.rosecloud.system.persistence.SystemSettingMapper;
import io.rosecloud.system.persistence.UserSettingEntity;
import io.rosecloud.system.persistence.UserSettingMapper;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingKeyServiceImplTest {
    @Mock
    SettingKeyMapper settingKeyMapper;
    @Mock
    SystemSettingMapper systemSettingMapper;
    @Mock
    UserSettingMapper userSettingMapper;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private SettingKeyServiceImpl service() {
        return new SettingKeyServiceImpl(settingKeyMapper, systemSettingMapper, userSettingMapper);
    }

    private static void setCurrentUser(Long userId, String username) {
        SecurityUser su = new SecurityUser(userId, username, null, null, true, null,
                new UserPrincipal(UserPrincipal.Type.USER_NAME, username), List.of());
        var auth = new UsernamePasswordAuthenticationToken(su, null, su.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createRejectsDuplicateKey() {
        when(settingKeyMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        BizException ex = assertThrows(BizException.class,
                () -> service().create(new SettingKeyCreateRequest("ui.theme", "主题", "desc")));
        assertEquals(SystemErrorCode.SETTING_KEY_EXISTS, ex.getErrorCode());
        verify(settingKeyMapper, never()).insert(any(SettingKeyEntity.class));
    }

    @Test
    void createStoresMetadataFromCurrentUser() {
        setCurrentUser(7L, "alice");
        when(settingKeyMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        service().create(new SettingKeyCreateRequest("ui.theme", "主题", "desc"));
        ArgumentCaptor<SettingKeyEntity> captor = ArgumentCaptor.forClass(SettingKeyEntity.class);
        verify(settingKeyMapper).insert(captor.capture());
        assertEquals("ui.theme", captor.getValue().getKey());
        assertEquals("主题", captor.getValue().getName());
        assertEquals("desc", captor.getValue().getRemark());
    }

    @Test
    void updateRejectsMissingKey() {
        when(settingKeyMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        BizException ex = assertThrows(BizException.class,
                () -> service().update("ui.theme", new SettingKeyUpdateRequest("主题", "desc")));
        assertEquals(SystemErrorCode.SETTING_KEY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void updateUsesPersistentIdOfExistingKey() {
        SettingKeyEntity existing = new SettingKeyEntity();
        existing.setId(88L);
        existing.setKey("ui.theme");
        existing.setName("主题");
        existing.setRemark("desc");
        when(settingKeyMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        service().update("ui.theme", new SettingKeyUpdateRequest("主题2", "desc2"));
        ArgumentCaptor<SettingKeyEntity> captor = ArgumentCaptor.forClass(SettingKeyEntity.class);
        verify(settingKeyMapper).updateById(captor.capture());
        assertEquals(88L, captor.getValue().getId());
        assertEquals("ui.theme", captor.getValue().getKey());
        assertEquals("主题2", captor.getValue().getName());
        assertEquals("desc2", captor.getValue().getRemark());
    }

    @Test
    void deleteCascadesToSystemAndUserSettings() {
        SettingKeyEntity existing = new SettingKeyEntity();
        existing.setId(1L);
        existing.setKey("ui.theme");
        when(settingKeyMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        service().delete("ui.theme");
        verify(systemSettingMapper).deleteById("ui.theme");
        verify(userSettingMapper).delete(any(LambdaQueryWrapper.class));
        verify(settingKeyMapper).deleteById(1L);
    }

    @Test
    void pageDelegatesToMapper() {
        IPage<SettingKeyEntity> page = mock(IPage.class);
        SettingKeyEntity e = new SettingKeyEntity();
        e.setId(1L);
        e.setKey("ui.theme");
        e.setName("主题");
        e.setRemark("desc");
        when(page.getRecords()).thenReturn(List.of(e));
        when(page.getTotal()).thenReturn(5L);
        when(page.getCurrent()).thenReturn(1L);
        when(page.getSize()).thenReturn(10L);
        when(settingKeyMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<SettingKey> result = service().page(1, 10, "ui");
        assertEquals(5L, result.total());
        assertEquals(1, result.records().size());
        assertEquals("ui.theme", result.records().get(0).getKey());
    }
}
