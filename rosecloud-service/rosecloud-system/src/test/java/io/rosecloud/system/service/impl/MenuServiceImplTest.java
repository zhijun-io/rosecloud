package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.event.EntityChangedEvent;
import io.rosecloud.common.core.event.EntityChangeType;
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.starter.data.event.EntityEventPublisher;
import io.rosecloud.system.domain.Menu;
import io.rosecloud.system.persistence.MenuDao;
import io.rosecloud.system.service.dto.MenuRequest;
import io.rosecloud.system.service.dto.MenuTreeNode;
import io.rosecloud.system.service.dto.UserMenuResult;
import io.rosecloud.system.service.validator.MenuValidator;
import org.junit.jupiter.api.AfterEach;
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
    MenuDao menuDao;
    @Mock
    MenuValidator menuValidator;
    @Mock
    EntityCache<Long, Menu> menuCache;
    @Mock
    EntityCache<String, List<Menu>> menuListCache;
    @Mock
    EntityCache<Long, List<Long>> roleMenuIdsCache;
    @Mock
    EntityEventPublisher eventPublisher;

    @Captor
    ArgumentCaptor<EntityChangedEvent<?>> eventCaptor;

    private MenuServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MenuServiceImpl(menuDao, menuValidator, menuCache, menuListCache,
                roleMenuIdsCache, eventPublisher);
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
        when(menuDao.save(any())).thenReturn(
                Menu.of(100L, 0L, "Test", 0, "/test", "test/index", "test", "test", 1, 1, 1));

        Long id = service.create(request);

        assertEquals(100L, id);
        verify(menuListCache).evictAll();
        verify(eventPublisher).publish(eventCaptor.capture());
        assertEquals(EntityChangeType.CREATED, eventCaptor.getValue().changeType());
        assertEquals(100L, eventCaptor.getValue().entityId());
    }

    @Test
    void updateEvictsListCacheAndPublishesEvent() {
        when(menuCache.getOrLoadTransactional(eq(1L), any())).thenReturn(
                Menu.of(1L, 0L, "Old", 0, "/old", "old/index", "old", "old", 1, 1, 1));
        when(menuDao.save(any())).thenReturn(
                Menu.of(1L, 0L, "Updated", 0, "/updated", "updated/index", "updated", "updated", 1, 1, 1));

        service.update(1L, new MenuRequest(0L, "Updated", 0, "/updated", "updated/index", "updated", "updated", 1, 1, 1));

        verify(menuListCache).evictAll();
        verify(eventPublisher).publish(eventCaptor.capture());
        assertEquals(EntityChangeType.UPDATED, eventCaptor.getValue().changeType());
        assertEquals(1L, eventCaptor.getValue().entityId());
    }

    @Test
    void deleteEvictsListCacheAndPublishesEvent() {
        when(menuDao.existsByParentId(1L)).thenReturn(false);

        service.delete(1L);

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
