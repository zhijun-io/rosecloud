package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.model.UserPrincipal;
import io.rosecloud.starter.security.session.LoginSessionApi;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.starter.data.event.EntityEventPublisher;
import io.rosecloud.system.domain.Role;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.RoleEntity;
import io.rosecloud.system.persistence.RoleMapper;
import io.rosecloud.system.persistence.RoleMenuEntity;
import io.rosecloud.system.persistence.RoleMenuMapper;
import io.rosecloud.system.persistence.UserRoleEntity;
import io.rosecloud.system.persistence.UserRoleMapper;
import io.rosecloud.system.service.dto.RoleCreateRequest;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    RoleMapper roleMapper;
    @Mock
    RoleMenuMapper roleMenuMapper;
    @Mock
    UserRoleMapper userRoleMapper;
    @Mock
    LoginSessionApi loginSessionApi;
    @Mock
    EntityCache<Long, List<Long>> roleMenuIdsCache;
    @Mock
    EntityEventPublisher eventPublisher;

    private RoleServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        TableInfoHelper.initTableInfo(assistant, RoleEntity.class);
        TableInfoHelper.initTableInfo(assistant, RoleMenuEntity.class);
        TableInfoHelper.initTableInfo(assistant, UserRoleEntity.class);
    }

    @BeforeEach
    void setUp() {
        service = new RoleServiceImpl(roleMapper, roleMenuMapper, userRoleMapper, loginSessionApi, roleMenuIdsCache, eventPublisher);
        // Platform admin context so security checks pass for role mutations
        SecurityUser admin = new SecurityUser(0L, "admin", "Admin", "", true,
                TenantContextHolder.SYSTEM_TENANT_ID,
                new UserPrincipal(UserPrincipal.Type.USER_NAME, "admin"),
                List.of(new SimpleGrantedAuthority("ROLE_admin")));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(admin, "", admin.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---- create ----

    @Test
    void createStoresRole() {
        when(roleMapper.exists(any())).thenReturn(false);
        doAnswer(invocation -> {
            RoleEntity po = invocation.getArgument(0);
            po.setId(1L);
            return 1;
        }).when(roleMapper).insert(any(RoleEntity.class));

        service.create(new RoleCreateRequest("admin", "Administrator"));

        verify(roleMapper).insert(any(RoleEntity.class));
        verify(eventPublisher).publish(any());
    }

    @Test
    void createRejectsDuplicateCode() {
        when(roleMapper.exists(any())).thenReturn(true);

        BizException ex = assertThrows(BizException.class,
                () -> service.create(new RoleCreateRequest("admin", "Administrator")));

        assertEquals(SystemErrorCode.ROLE_CODE_EXISTS, ex.getErrorCode());
        verify(roleMapper, never()).insert(any(RoleEntity.class));
    }

    // ---- get ----

    @Test
    void getReturnsRoleWhenExists() {
        RoleEntity entity = new RoleEntity();
        entity.setId(1L);
        entity.setCode("admin");
        entity.setName("Administrator");
        entity.setCreateTime(LocalDateTime.now());
        when(roleMapper.selectById(1L)).thenReturn(entity);

        Role role = service.get(1L);

        assertEquals(1L, role.getId());
        assertEquals("admin", role.getCode());
        assertEquals("Administrator", role.getName());
    }

    @Test
    void getThrowsWhenNotFound() {
        when(roleMapper.selectById(99L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> service.get(99L));

        assertEquals(SystemErrorCode.ROLE_NOT_FOUND, ex.getErrorCode());
    }

    // ---- assignMenus ----

    @Test
    void assignMenusRejectsMissingRole() {
        when(roleMapper.selectById(99L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> service.assignMenus(99L, List.of(1L, 2L)));

        assertEquals(SystemErrorCode.ROLE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void assignMenusClearsPreviousAndInsertsNew() {
        RoleEntity entity = new RoleEntity();
        entity.setId(1L);
        entity.setCode("admin");
        when(roleMapper.selectById(1L)).thenReturn(entity);
        when(userRoleMapper.selectList(any())).thenReturn(List.of());

        service.assignMenus(1L, List.of(10L, 20L));

        verify(roleMenuMapper).delete(any());
        verify(roleMenuMapper, org.mockito.Mockito.times(2)).insert(any(RoleMenuEntity.class));
    }

    @Test
    void assignMenusRevokesSessionsForRoleHolders() {
        RoleEntity entity = new RoleEntity();
        entity.setId(1L);
        entity.setCode("admin");
        when(roleMapper.selectById(1L)).thenReturn(entity);
        UserRoleEntity userRel = new UserRoleEntity();
        userRel.setUserId(100L);
        UserRoleEntity userRel2 = new UserRoleEntity();
        userRel2.setUserId(200L);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRel, userRel2));

        service.assignMenus(1L, List.of(10L));

        verify(loginSessionApi).revokeByUserId(100L);
        verify(loginSessionApi).revokeByUserId(200L);
    }

    // ---- findMenuIdsByRoleId ----

    @Test
    void findMenuIdsByRoleIdDelegatesToMapper() {
        RoleMenuEntity rm1 = new RoleMenuEntity();
        rm1.setMenuId(10L);
        RoleMenuEntity rm2 = new RoleMenuEntity();
        rm2.setMenuId(20L);
        when(roleMenuMapper.selectList(any())).thenReturn(List.of(rm1, rm2));

        List<Long> menuIds = service.findMenuIdsByRoleId(1L);

        assertEquals(List.of(10L, 20L), menuIds);
    }

    @Test
    void findMenuIdsByRoleIdReturnsEmptyList() {
        when(roleMenuMapper.selectList(any())).thenReturn(List.of());

        List<Long> menuIds = service.findMenuIdsByRoleId(1L);

        assertTrue(menuIds.isEmpty());
    }
}
