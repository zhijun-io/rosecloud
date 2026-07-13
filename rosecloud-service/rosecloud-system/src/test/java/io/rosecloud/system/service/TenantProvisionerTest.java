package io.rosecloud.system.service;

import io.rosecloud.api.notice.NoticePublishApi;
import io.rosecloud.api.notice.NoticePublishRequest;
import io.rosecloud.api.notice.NoticeTargetType;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.system.domain.Role;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.error.SystemErrorCode;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.rosecloud.system.persistence.RoleEntity;
import io.rosecloud.system.persistence.RoleMapper;
import io.rosecloud.system.persistence.TenantEntity;
import io.rosecloud.system.persistence.TenantMapper;
import io.rosecloud.system.persistence.TenantProfileEntity;
import io.rosecloud.system.service.dto.UserActivationInfo;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantProvisionerTest {

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        TableInfoHelper.initTableInfo(assistant, TenantEntity.class);
        TableInfoHelper.initTableInfo(assistant, TenantProfileEntity.class);
        TableInfoHelper.initTableInfo(assistant, RoleEntity.class);
    }

    @Mock
    TenantMapper tenantMapper;
    @Mock
    RoleMapper roleMapper;
    @Mock
    UserService userService;
    @Mock
    UserActivationService userActivationService;
    @Mock
    NoticePublishApi noticePublishApi;

    private TenantProvisioner service() {
        return new TenantProvisioner(tenantMapper, roleMapper, userService, userActivationService,
                noticePublishApi);
    }

    @Test
    void provisionEnablesTenantWithoutAdminUsername() {
        when(tenantMapper.selectById("TENANT1")).thenReturn(withAdmin("TENANT1", " "));
        when(noticePublishApi.publish(any(NoticePublishRequest.class))).thenReturn(1L);

        service().provision("TENANT1");

        verify(tenantMapper).update(any(), any());
        verify(userService, never()).createWithoutPassword(any(), any(), any());
        verify(userActivationService, never()).resend(any());
    }

    @Test
    void provisionCreatesAdminAndAssignsRolesWhenUsernameExists() {
        when(tenantMapper.selectById("TENANT2")).thenReturn(withAdmin("TENANT2", "admin"));
        when(roleMapper.selectOne(any())).thenReturn(role(7L, "tenant-admin", "Tenant admin"));
        when(userService.createWithoutPassword("admin", "admin", "TENANT2")).thenReturn(88L);
        doNothing().when(userActivationService).resend("admin");
        when(noticePublishApi.publish(any(NoticePublishRequest.class))).thenReturn(1L);

        service().provision("TENANT2");

        verify(userService).createWithoutPassword("admin", "admin", "TENANT2");
        verify(userService).assignRoles(88L, List.of(7L));
        verify(userActivationService).resend("admin");
        ArgumentCaptor<NoticePublishRequest> noticeCaptor = ArgumentCaptor.forClass(NoticePublishRequest.class);
        verify(noticePublishApi).publish(noticeCaptor.capture());
        assertEquals(NoticeTargetType.TENANT.code(), noticeCaptor.getValue().targetType());
        assertEquals("TENANT2", noticeCaptor.getValue().targetTenantId());
        assertEquals(null, noticeCaptor.getValue().targetRoleCode());
        assertEquals(null, noticeCaptor.getValue().targetUsername());
    }

    @Test
    void provisionFailsWhenTenantAdminRoleMissing() {
        when(tenantMapper.selectById("TENANT3")).thenReturn(withAdmin("TENANT3", "admin"));
        when(roleMapper.selectOne(any())).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> service().provision("TENANT3"));

        assertEquals(SystemErrorCode.ROLE_NOT_FOUND, ex.getErrorCode());
        verify(tenantMapper, never()).update(any(), any());
        verify(userService, never()).createWithoutPassword(any(), any(), any());
    }

    private static TenantEntity withAdmin(String id, String admin) {
        TenantEntity e = new TenantEntity();
        e.setId(id);
        e.setAdminUsername(admin);
        return e;
    }

    private static RoleEntity role(long id, String code, String name) {
        RoleEntity e = new RoleEntity();
        e.setId(id);
        e.setCode(code);
        e.setName(name);
        return e;
    }
}
