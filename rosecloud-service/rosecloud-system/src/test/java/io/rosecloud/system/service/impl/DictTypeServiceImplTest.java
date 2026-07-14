package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.rosecloud.common.core.event.EntityChangedEvent;
import io.rosecloud.common.core.event.EntityChangeType;
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.starter.data.event.EntityEventPublisher;
import io.rosecloud.system.domain.DictType;
import io.rosecloud.system.persistence.DictDataEntity;
import io.rosecloud.system.persistence.DictDataDao;
import io.rosecloud.system.persistence.DictTypeDao;
import io.rosecloud.system.persistence.DictTypeEntity;
import io.rosecloud.system.persistence.DictTypeMapper;
import io.rosecloud.system.service.dto.DictTypeRequest;
import io.rosecloud.system.service.validator.DictTypeValidator;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DictTypeServiceImplTest {

    @Mock
    DictTypeDao dictTypeDao;
    @Mock
    DictDataDao dictDataDao;
    @Mock
    DictTypeValidator dictTypeValidator;
    @Mock
    EntityCache<Long, DictType> dictTypeCache;
    @Mock
    EntityCache<String, List<io.rosecloud.system.domain.DictData>> dictDataByCodeCache;
    @Mock
    EntityEventPublisher eventPublisher;

    @Captor
    ArgumentCaptor<EntityChangedEvent<?>> eventCaptor;

    private DictTypeServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        TableInfoHelper.initTableInfo(assistant, DictTypeEntity.class);
        TableInfoHelper.initTableInfo(assistant, DictDataEntity.class);
    }

    @BeforeEach
    void setUp() {
        service = new DictTypeServiceImpl(dictTypeDao, dictDataDao, dictTypeValidator,
                dictTypeCache, dictDataByCodeCache, eventPublisher);
    }

    @Test
    void getReturnsCachedDictType() {
        DictType cached = DictType.of(1L, "gender", "Gender", 1, "Gender dict");
        when(dictTypeCache.getOrLoadTransactional(eq(1L), any())).thenReturn(cached);

        DictType result = service.get(1L);

        assertEquals(1L, result.getId());
        assertEquals("gender", result.getCode());
    }

    @Test
    void getThrowsWhenNotFound() {
        when(dictTypeCache.getOrLoadTransactional(eq(99L), any())).thenReturn(null);

        assertThrows(io.rosecloud.common.core.error.BizException.class,
                () -> service.get(99L));
    }

    @Test
    void createEvictsAndPublishesEvent() {
        DictTypeRequest request = new DictTypeRequest("gender", "Gender", 1, "Gender dict");
        when(dictTypeDao.save(any())).thenReturn(
                DictType.of(100L, "gender", "Gender", 1, "Gender dict"));

        Long id = service.create(request);

        assertEquals(100L, id);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertEquals(EntityChangeType.CREATED, eventCaptor.getValue().changeType());
        assertEquals(100L, eventCaptor.getValue().entityId());
    }

    @Test
    void updatePublishesEvent() {
        when(dictTypeDao.findById(1L)).thenReturn(
                Optional.of(DictType.of(1L, "gender", "Gender", 1, "Gender dict")));
        when(dictTypeDao.save(any())).thenReturn(
                DictType.of(1L, "gender", "Updated", 1, "Updated"));

        service.update(1L, new DictTypeRequest("gender", "Updated", 1, "Updated"));

        verify(eventPublisher).publish(eventCaptor.capture());
        assertEquals(EntityChangeType.UPDATED, eventCaptor.getValue().changeType());
        assertEquals(1L, eventCaptor.getValue().entityId());
    }

    @Test
    void deleteEvictsDictDataCacheAndPublishesEvent() {
        when(dictTypeDao.findById(1L)).thenReturn(
                Optional.of(DictType.of(1L, "gender", "Gender", 1, "Gender dict")));

        service.delete(1L);

        verify(dictDataByCodeCache).evict("gender");
        verify(eventPublisher).publish(eventCaptor.capture());
        assertEquals(EntityChangeType.DELETED, eventCaptor.getValue().changeType());
        assertEquals(1L, eventCaptor.getValue().entityId());
    }
}
