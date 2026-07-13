package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.rosecloud.common.core.event.EntityChangedEvent;
import io.rosecloud.common.core.event.EntityChangeType;
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.starter.data.event.EntityEventPublisher;
import io.rosecloud.system.domain.DictData;
import io.rosecloud.system.persistence.DictDataEntity;
import io.rosecloud.system.persistence.DictDataMapper;
import io.rosecloud.system.service.dto.DictDataRequest;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DictDataServiceImplTest {

    @Mock
    DictDataMapper dictDataMapper;
    @Mock
    EntityCache<String, List<DictData>> dictDataByCodeCache;
    @Mock
    EntityEventPublisher eventPublisher;

    @Captor
    ArgumentCaptor<EntityChangedEvent<?>> eventCaptor;

    private DictDataServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        TableInfoHelper.initTableInfo(assistant, DictDataEntity.class);
    }

    @BeforeEach
    void setUp() {
        service = new DictDataServiceImpl(dictDataMapper, dictDataByCodeCache, eventPublisher);
    }

    @Test
    void listByCodeDelegatesToCache() {
        List<DictData> cached = List.of(
                DictData.of(1L, "gender", "Male", "0", 1, 1, "Male gender")
        );
        when(dictDataByCodeCache.getOrLoad(eq("gender"), any())).thenReturn(cached);

        List<DictData> result = service.listByCode("gender");

        assertEquals(1, result.size());
        assertEquals("Male", result.get(0).getLabel());
    }

    @Test
    void createEvictsCacheAndPublishesEvent() {
        DictDataRequest request = new DictDataRequest("gender", "Male", "0", 1, 1, "Male gender");
        when(dictDataMapper.insert(any(DictDataEntity.class))).thenAnswer(invocation -> {
            DictDataEntity po = invocation.getArgument(0);
            po.setId(100L);
            return 1;
        });

        Long id = service.create(request);

        assertEquals(100L, id);
        verify(dictDataByCodeCache).evictAll();
        verify(eventPublisher).publish(eventCaptor.capture());
        assertEquals(EntityChangeType.CREATED, eventCaptor.getValue().changeType());
        assertEquals(100L, eventCaptor.getValue().entityId());
    }

    @Test
    void updateEvictsCacheAndPublishesEvent() {
        DictDataEntity existing = new DictDataEntity();
        existing.setId(1L);
        existing.setDictCode("gender");
        existing.setLabel("Old");
        existing.setValue("0");
        existing.setSort(1);
        existing.setStatus(1);
        when(dictDataMapper.selectById(1L)).thenReturn(existing);

        service.update(1L, new DictDataRequest("gender", "Updated", "0", 1, 1, "Updated"));

        verify(dictDataByCodeCache).evictAll();
        verify(eventPublisher).publish(eventCaptor.capture());
        assertEquals(EntityChangeType.UPDATED, eventCaptor.getValue().changeType());
        assertEquals(1L, eventCaptor.getValue().entityId());
    }

    @Test
    void deleteEvictsCacheAndPublishesEvent() {
        service.delete(1L);

        verify(dictDataByCodeCache).evictAll();
        verify(eventPublisher).publish(eventCaptor.capture());
        assertEquals(EntityChangeType.DELETED, eventCaptor.getValue().changeType());
        assertEquals(1L, eventCaptor.getValue().entityId());
    }

    @Test
    void getThrowsWhenNotFound() {
        when(dictDataMapper.selectById(99L)).thenReturn(null);

        assertThrows(io.rosecloud.common.core.error.BizException.class,
                () -> service.get(99L));
    }
}
