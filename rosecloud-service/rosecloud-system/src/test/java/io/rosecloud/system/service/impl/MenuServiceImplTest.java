package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.rosecloud.common.core.event.EntityChangedEvent;
import io.rosecloud.common.core.event.EntityChangeType;
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.starter.data.event.EntityEventPublisher;
import io.rosecloud.system.domain.Menu;
import io.rosecloud.system.persistence.MenuEntity;
import io.rosecloud.system.persistence.MenuMapper;
import io.rosecloud.system.persistence.RoleMenuEntity;
import io.rosecloud.system.persistence.RoleMenuMapper;
import io.rosecloud.system.persistence.UserRoleEntity;
import io.rosecloud.system.persistence.UserRoleMapper;
import io.rosecloud.system.service.dto.MenuRequest;
import io.rosecloud.system.service.dto.MenuTreeNode;
import io.rosecloud.system.service.dto.UserMenuResult;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {

    @Mock
    MenuMapper menuMapper;
    @Mock
    RoleMenuMapper roleMenuMapper;
    @Mock
    UserRoleMapper userRoleMapper;
    @Mock
    EntityCache<Long, Menu> menuCache;
    @Mock
    EntityCache<String, List<Menu>> menuListCache;
    @Mock
    EntityEventPublisher eventPublisher;

    @Captor
    ArgumentCaptor<EntityChangedEvent<?>> eventCaptor;

    private MenuServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        TableInfoHelper.initTableInfo(assistant, MenuEntity.class);
        TableInfoHelper.initTableInfo(assistant, RoleMenuEntity.class);
        TableInfoHelper.initTableInfo(assistant, UserRoleEntity.class);
    }

    @BeforeEach
    void setUp() {
        service = new MenuServiceImpl(menuMapper, roleMenuMapper, userRoleMapper,
                menuCache, menuListCache, eventPublisher);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listDelegatesToMenuListCache() {
        List<Menu> cached = List.of(
                Menu.of(1L, 0L, "Dashboard", 0, "/dashboard", "dashboard/index", "dashboard", "dashboard", 1, 1, 1)
        );
        when(menuListCache.getOrLoad(eq("__all__"), any())).thenReturn(cached);

        List<Menu> result = service.list();

        assertEquals(1, result.size());
        assertEquals("Dashboard", result.get(0).getName());
    }

    @Test
    void createEvictsMenuListCacheAndPublishesEvent() {
        MenuRequest request = new MenuRequest(0L, "Test", 0, "/test", "test/index", "test", "test", 1, 1, 1);
        when(menuMapper.insert(any(MenuEntity.class))).thenAnswer(invocation -> {
            MenuEntity po = invocation.getArgument(0);
            po.setId(100L);
            return 1;
        });

        Long id = service.create(request);

        assertEquals(100L, id);
        verify(menuListCache).evictAll();
        verify(eventPublisher).publish(eventCaptor.capture());
        assertEquals(EntityChangeType.CREATED, eventCaptor.getValue().changeType());
        assertEquals(100L, eventCaptor.getValue().entityId());
    }

    @Test
    void updateEvictsBothCachesAndPublishesEvent() {
        MenuEntity entity = new MenuEntity();
        entity.setId(1L);
        entity.setName("Old");
        entity.setParentId(0L);
        entity.setType(0);
        entity.setPath("/old");
        entity.setComponent("old/index");
        entity.setPerms("old");
        entity.setIcon("old");
        entity.setSort(1);
        entity.setStatus(1);
        entity.setVisible(1);
        when(menuMapper.selectById(1L)).thenReturn(entity);

        service.update(1L, new MenuRequest(0L, "Updated", 0, "/updated", "updated/index", "updated", "updated", 1, 1, 1));

        verify(menuCache).evict(1L);
        verify(menuListCache).evictAll();
        verify(eventPublisher).publish(eventCaptor.capture());
        assertEquals(EntityChangeType.UPDATED, eventCaptor.getValue().changeType());
        assertEquals(1L, eventCaptor.getValue().entityId());
    }

    @Test
    void deleteEvictsBothCachesAndPublishesEvent() {
        when(menuMapper.exists(any())).thenReturn(false);

        service.delete(1L);

        verify(menuCache).evict(1L);
        verify(menuListCache).evictAll();
        verify(eventPublisher).publish(eventCaptor.capture());
        assertEquals(EntityChangeType.DELETED, eventCaptor.getValue().changeType());
        assertEquals(1L, eventCaptor.getValue().entityId());
    }

    @Test
    void treeBuildsFromList() {
        List<Menu> menus = List.of(
                Menu.of(1L, 0L, "Dashboard", 0, "/dashboard", "dashboard/index", "dashboard", "dashboard", 1, 1, 1)
        );
        when(menuListCache.getOrLoad(eq("__all__"), any())).thenReturn(menus);

        List<MenuTreeNode> tree = service.tree();

        assertEquals(1, tree.size());
        assertEquals("Dashboard", tree.get(0).menu().getName());
    }

    @Test
    void myMenusReturnsEmptyWhenNoAuthentication() {
        SecurityContextHolder.clearContext();

        UserMenuResult result = service.myMenus();

        assertTrue(result.menus().isEmpty());
        assertTrue(result.permissions().isEmpty());
    }
}
