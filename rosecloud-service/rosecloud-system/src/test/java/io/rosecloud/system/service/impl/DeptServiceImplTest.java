package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.rosecloud.common.core.event.EntityChangedEvent;
import io.rosecloud.common.core.event.EntityChangeType;
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.starter.data.event.EntityEventPublisher;
import io.rosecloud.system.domain.Dept;
import io.rosecloud.system.persistence.DeptEntity;
import io.rosecloud.system.persistence.DeptMapper;
import io.rosecloud.system.service.dto.DeptRequest;
import io.rosecloud.system.service.dto.DeptTreeNode;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeptServiceImplTest {

    @Mock
    DeptMapper deptMapper;
    @Mock
    EntityCache<Long, Dept> deptCache;
    @Mock
    EntityCache<String, List<Dept>> deptListCache;
    @Mock
    EntityEventPublisher eventPublisher;

    @Captor
    ArgumentCaptor<EntityChangedEvent<?>> eventCaptor;

    private DeptServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        TableInfoHelper.initTableInfo(assistant, DeptEntity.class);
    }

    @BeforeEach
    void setUp() {
        service = new DeptServiceImpl(deptMapper, deptCache, deptListCache, eventPublisher);
    }

    @Test
    void listDelegatesToDeptListCache() {
        List<Dept> cached = List.of(
                Dept.of(1L, 0L, "HQ", 1, 1, "Leader", "123456")
        );
        when(deptListCache.getOrLoad(eq("__all__"), any())).thenReturn(cached);

        List<Dept> result = service.list();

        assertEquals(1, result.size());
        assertEquals("HQ", result.get(0).getName());
    }

    @Test
    void createEvictsDeptListCacheAndPublishesEvent() {
        DeptRequest request = new DeptRequest(0L, "HQ", 1, 1, "Leader", "123456");
        when(deptMapper.insert(any(DeptEntity.class))).thenAnswer(invocation -> {
            DeptEntity po = invocation.getArgument(0);
            po.setId(100L);
            return 1;
        });

        Long id = service.create(request);

        assertEquals(100L, id);
        verify(deptListCache).evictAll();
        verify(eventPublisher).publish(eventCaptor.capture());
        assertEquals(EntityChangeType.CREATED, eventCaptor.getValue().changeType());
        assertEquals(100L, eventCaptor.getValue().entityId());
    }

    @Test
    void updateEvictsBothCachesAndPublishesEvent() {
        DeptEntity existing = new DeptEntity();
        existing.setId(1L);
        existing.setParentId(0L);
        existing.setName("Old");
        existing.setSort(1);
        existing.setStatus(1);
        when(deptMapper.selectById(1L)).thenReturn(existing);

        service.update(1L, new DeptRequest(0L, "Updated", 1, 1, "Leader", "123456"));

        verify(deptCache).evict(1L);
        verify(deptListCache).evictAll();
        verify(eventPublisher).publish(eventCaptor.capture());
        assertEquals(EntityChangeType.UPDATED, eventCaptor.getValue().changeType());
        assertEquals(1L, eventCaptor.getValue().entityId());
    }

    @Test
    void deleteEvictsBothCachesAndPublishesEvent() {
        when(deptMapper.exists(any())).thenReturn(false);

        service.delete(1L);

        verify(deptCache).evict(1L);
        verify(deptListCache).evictAll();
        verify(eventPublisher).publish(eventCaptor.capture());
        assertEquals(EntityChangeType.DELETED, eventCaptor.getValue().changeType());
        assertEquals(1L, eventCaptor.getValue().entityId());
    }

    @Test
    void treeBuildsFromList() {
        List<Dept> depts = List.of(
                Dept.of(1L, 0L, "HQ", 1, 1, "Leader", "123456")
        );
        when(deptListCache.getOrLoad(eq("__all__"), any())).thenReturn(depts);

        List<DeptTreeNode> tree = service.tree();

        assertEquals(1, tree.size());
        assertEquals("HQ", tree.get(0).dept().getName());
    }
}
