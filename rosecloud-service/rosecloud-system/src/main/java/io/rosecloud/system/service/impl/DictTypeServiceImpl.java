package io.rosecloud.system.service.impl;
import lombok.RequiredArgsConstructor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.event.EntityChangedEvent;
import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.SortDirection;
import io.rosecloud.common.core.model.SortField;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.starter.data.EntityCacheNames;
import io.rosecloud.starter.data.PagedResults;
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.starter.data.event.EntityEventPublisher;
import io.rosecloud.system.domain.DictType;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.DictDataEntity;
import io.rosecloud.system.persistence.DictDataMapper;
import io.rosecloud.system.persistence.DictTypeEntity;
import io.rosecloud.system.persistence.DictTypeMapper;
import io.rosecloud.system.service.DictTypeService;
import io.rosecloud.system.service.dto.DictTypeRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class DictTypeServiceImpl implements DictTypeService {

    private final DictTypeMapper dictTypeMapper;
    private final DictDataMapper dictDataMapper;
    private final EntityCache<Long, DictType> dictTypeCache;
    private final EntityEventPublisher eventPublisher;
    @AuditLog(action = "dict-type-create", description = "创建字典类型")
    @Override
    public Long create(DictTypeRequest request) {
        if (dictTypeMapper.exists(new LambdaQueryWrapper<DictTypeEntity>().eq(DictTypeEntity::getCode, request.code()))) {
            throw new BizException(SystemErrorCode.DICT_TYPE_CODE_EXISTS);
        }
        DictTypeEntity po = new DictTypeEntity().toEntity(DictType.of(null, request.code(), request.name(),
                request.status() == null ? 1 : request.status(), request.remark()));
        po.setId(null);
        dictTypeMapper.insert(po);
        eventPublisher.publish(EntityChangedEvent.created(
                EntityCacheNames.DICT_TYPE, po.getId(), null, null));
        return po.getId();
    }

    @AuditLog(action = "dict-type-update", description = "修改字典类型")
    @Override
    public void update(Long id, DictTypeRequest request) {
        DictType existing = findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.DICT_TYPE_NOT_FOUND));
        DictTypeEntity po = new DictTypeEntity().toEntity(DictType.of(id, request.code(), request.name(),
                request.status() == null ? existing.getStatus() : request.status(), request.remark()));
        dictTypeMapper.updateById(po);
        dictTypeCache.evict(id);
        eventPublisher.publish(EntityChangedEvent.updated(
                EntityCacheNames.DICT_TYPE, id, null, null, null));
    }

    @AuditLog(action = "dict-type-delete", description = "删除字典类型")
    @Transactional
    @Override
    public void delete(Long id) {
        DictType dictType = findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.DICT_TYPE_NOT_FOUND));
        dictDataMapper.delete(new LambdaQueryWrapper<DictDataEntity>().eq(DictDataEntity::getDictCode, dictType.getCode()));
        dictTypeMapper.deleteById(id);
        dictTypeCache.evict(id);
        eventPublisher.publish(EntityChangedEvent.deleted(
                EntityCacheNames.DICT_TYPE, id, null, null));
    }

    @Override
    public DictType get(Long id) {
        return findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.DICT_TYPE_NOT_FOUND));
    }

    @Override
    public PagedData<DictType> page(PageQuery pageQuery) {
        return PagedResults.page(pageQuery, DictTypeEntity.class, dictTypeMapper,
                q -> {
                    LambdaQueryWrapper<DictTypeEntity> wrapper = new LambdaQueryWrapper<>();
                    if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
                        wrapper.like(DictTypeEntity::getCode, q.getKeyword())
                                .or().like(DictTypeEntity::getName, q.getKeyword());
                    }
                    return wrapper;
                },
                SortField.of("createTime", SortDirection.DESC));
    }

    private Optional<DictType> findById(Long id) {
        return Optional.ofNullable(dictTypeCache.getOrLoad(id, () ->
                Optional.ofNullable(dictTypeMapper.selectById(id)).map(DictTypeEntity::toData).orElse(null)
        ));
    }

}
