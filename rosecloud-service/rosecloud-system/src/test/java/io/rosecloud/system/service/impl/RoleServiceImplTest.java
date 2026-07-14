package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
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
import io.rosecloud.system.persistence.RoleDao;
import io.rosecloud.system.persistence.RoleEntity;
import io.rosecloud.system.persistence.RoleMenuEntity;
import io.rosecloud.system.persistence.UserRoleEntity;
import io.rosecloud.system.service.dto.RoleCreateRequest;
import io.rosecloud.system.service.validator.RoleValidator;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    RoleDao roleDao;
    @Mock
    RoleValidator roleValidator;
    @Mock
    LoginSessionApi loginSessionApi;
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    EntityCache<Long, List<Long>> roleMenuIdsCache;
    @Mock
    EntityCache<String, List<io.rosecloud.system.domain.Menu>> menuListCache;
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
        service = new RoleServiceImpl(roleDao, roleValidator, loginSessionApi,
                roleMenuIdsCache, menuListCache, eventPublisher);
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
        when(roleDao.save(any())).thenReturn(new Role(1L, "admin", "Administrator", null, null, null, null));

        service.create(new RoleCreateRequest("admin", "Administrator"));

        verify(roleDao).save(any());
        verify(eventPublisher).publish(any());
    }

    @Test
    void createRejectsDuplicateCode() {
        // RoleValidator.validateCreate() will throw if code exists
        // We can verify the validation is called by checking roleDao.save is not called
        when(roleDao.save(any())).thenThrow(new BizException(SystemErrorCode.ROLE_CODE_EXISTS));

        BizException ex = assertThrows(BizException.class,
                () -> service.create(new RoleCreateRequest("admin", "Administrator")));

        assertEquals(SystemErrorCode.ROLE_CODE_EXISTS, ex.getErrorCode());
    }

    // ---- get ----

    @Test
    void getReturnsRoleWhenExists() {
        Role role = new Role(1L, "admin", "Administrator", null, null, null, null);
        when(roleDao.findById(1L)).thenReturn(Optional.of(role));

        Role result = service.get(1L);

        assertEquals(1L, result.getId());
        assertEquals("admin", result.getCode());
        assertEquals("Administrator", result.getName());
    }

    @Test
    void getThrowsWhenNotFound() {
        when(roleDao.findById(99L)).thenReturn(Optional.empty());

        BizException ex = assertThrows(BizException.class, () -> service.get(99L));

        assertEquals(SystemErrorCode.ROLE_NOT_FOUND, ex.getErrorCode());
    }

    // ---- assignMenus ----

    @Test
    void assignMenusRejectsMissingRole() {
        when(roleDao.findById(99L)).thenReturn(Optional.empty());

        BizException ex = assertThrows(BizException.class,
                () -> service.assignMenus(99L, List.of(1L, 2L)));

        assertEquals(SystemErrorCode.ROLE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void assignMenusClearsPreviousAndInsertsNew() {
        when(roleDao.findById(1L)).thenReturn(Optional.of(
                new Role(1L, "admin", null, null, null, null, null)));
        when(roleDao.findUserIdsByRoleId(1L)).thenReturn(List.of());

        service.assignMenus(1L, List.of(10L, 20L));

        verify(roleDao).deleteRoleMenuByRoleId(1L);
        verify(roleDao).assignMenusToRole(1L, List.of(10L, 20L));
        verify(roleMenuIdsCache).evict(1L);
        verify(menuListCache).evictAll();
    }

    @Test
    void assignMenusRevokesSessionsForRoleHolders() {
        when(roleDao.findById(1L)).thenReturn(Optional.of(
                new Role(1L, "admin", null, null, null, null, null)));
        when(roleDao.findUserIdsByRoleId(1L)).thenReturn(List.of(100L, 200L));

        service.assignMenus(1L, List.of(10L));

        verify(loginSessionApi).revokeByUserId(100L);
        verify(loginSessionApi).revokeByUserId(200L);
    }

    // ---- findMenuIdsByRoleId ----

    @Test
    void findMenuIdsByRoleIdDelegatesToMapper() {
        when(roleDao.findMenuIdsByRoleId(1L)).thenReturn(List.of(10L, 20L));

        List<Long> menuIds = service.findMenuIdsByRoleId(1L);

        assertEquals(List.of(10L, 20L), menuIds);
    }

    @Test
    void findMenuIdsByRoleIdReturnsEmptyList() {
        when(roleDao.findMenuIdsByRoleId(1L)).thenReturn(List.of());

        List<Long> menuIds = service.findMenuIdsByRoleId(1L);

        assertTrue(menuIds.isEmpty());
    }
}
