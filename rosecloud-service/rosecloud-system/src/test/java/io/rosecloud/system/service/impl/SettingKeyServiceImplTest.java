package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.SortField;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.model.UserPrincipal;
import io.rosecloud.system.domain.SettingKey;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.SettingKeyDao;
import io.rosecloud.system.persistence.SettingKeyEntity;
import io.rosecloud.system.persistence.SystemSettingDao;
import io.rosecloud.system.persistence.UserSettingEntity;
import io.rosecloud.system.persistence.UserSettingMapper;
import io.rosecloud.system.service.dto.SettingKeyCreateRequest;
import io.rosecloud.system.service.dto.SettingKeyUpdateRequest;
import io.rosecloud.system.service.validator.SettingKeyValidator;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingKeyServiceImplTest {
    @Mock
    SettingKeyDao settingKeyDao;
    @Mock
    SettingKeyValidator settingKeyValidator;
    @Mock
    SystemSettingDao systemSettingDao;
    @Mock
    UserSettingMapper userSettingMapper;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        TableInfoHelper.initTableInfo(assistant, SettingKeyEntity.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private SettingKeyServiceImpl service() {
        return new SettingKeyServiceImpl(settingKeyDao, settingKeyValidator, userSettingMapper, systemSettingDao);
    }

    private static void setCurrentUser(Long userId, String username) {
        SecurityUser su = new SecurityUser(userId, username, null, null, true, null,
                new UserPrincipal(UserPrincipal.Type.USER_NAME, username), List.of());
        var auth = new UsernamePasswordAuthenticationToken(su, null, su.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createRejectsDuplicateKey() {
        doThrow(new BizException(SystemErrorCode.SETTING_KEY_EXISTS))
                .when(settingKeyValidator).validateCreate(any());

        BizException ex = assertThrows(BizException.class,
                () -> service().create(new SettingKeyCreateRequest("ui.theme", "主题", "desc")));
        assertEquals(SystemErrorCode.SETTING_KEY_EXISTS, ex.getErrorCode());
        verify(settingKeyDao, never()).save(any());
    }

    @Test
    void createStoresMetadataFromCurrentUser() {
        setCurrentUser(7L, "alice");
        when(settingKeyDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service().create(new SettingKeyCreateRequest("ui.theme", "主题", "desc"));

        ArgumentCaptor<SettingKey> captor = ArgumentCaptor.forClass(SettingKey.class);
        verify(settingKeyDao).save(captor.capture());
        SettingKey saved = captor.getValue();
        assertEquals("ui.theme", saved.getKey());
        assertEquals("主题", saved.getName());
        assertEquals("desc", saved.getRemark());
    }

    @Test
    void updateRejectsMissingKey() {
        when(settingKeyDao.findByKey("ui.theme")).thenReturn(Optional.empty());

        BizException ex = assertThrows(BizException.class,
                () -> service().update("ui.theme", new SettingKeyUpdateRequest("主题", "desc")));
        assertEquals(SystemErrorCode.SETTING_KEY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void updateUsesPersistentIdOfExistingKey() {
        when(settingKeyDao.findByKey("ui.theme")).thenReturn(Optional.of(new SettingKey(88L, "ui.theme", "主题", "desc", null, null, null, null)));
        when(settingKeyDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        setCurrentUser(7L, "alice");

        service().update("ui.theme", new SettingKeyUpdateRequest("主题2", "desc2"));

        ArgumentCaptor<SettingKey> captor = ArgumentCaptor.forClass(SettingKey.class);
        verify(settingKeyDao).save(captor.capture());
        SettingKey updated = captor.getValue();
        assertEquals(88L, updated.getId());
        assertEquals("ui.theme", updated.getKey());
        assertEquals("主题2", updated.getName());
        assertEquals("desc2", updated.getRemark());
    }

    @Test
    void deleteCascadesToSystemAndUserSettings() {
        when(settingKeyDao.findByKey("ui.theme")).thenReturn(Optional.of(new SettingKey(1L, "ui.theme", null, null, null, null, null, null)));

        service().delete("ui.theme");

        verify(systemSettingDao).removeById("ui.theme");
        verify(userSettingMapper).delete(any(LambdaQueryWrapper.class));
        verify(settingKeyDao).removeById(1L);
    }

    @Test
    void pageDelegatesToDao() {
        SettingKey sk = new SettingKey(1L, "ui.theme", "主题", "desc", null, null, null, null);
        PagedData<SettingKey> paged = new PagedData<>(List.of(sk, sk, sk, sk, sk), 1, 5L, false);
        when(settingKeyDao.page(any(), any(), any(), any())).thenReturn(paged);

        PagedData<SettingKey> result = service().page(new PageQuery(1, 10, "ui", List.of()));
        assertEquals(5L, result.getTotalElements());
        assertEquals(5, result.getData().size());
        assertEquals("ui.theme", result.getData().get(0).getKey());
    }
}
