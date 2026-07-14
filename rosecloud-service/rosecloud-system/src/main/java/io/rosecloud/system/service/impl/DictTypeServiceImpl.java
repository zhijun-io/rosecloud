package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.event.EntityChangedEvent;
import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.SortDirection;
import io.rosecloud.common.core.model.SortField;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.starter.data.EntityCacheNames;
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.starter.data.event.EntityEventPublisher;
import io.rosecloud.system.domain.DictData;
import io.rosecloud.system.domain.DictType;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.DictDataDao;
import io.rosecloud.system.persistence.DictTypeDao;
import io.rosecloud.system.persistence.DictTypeEntity;
import io.rosecloud.system.service.DictTypeService;
import io.rosecloud.system.service.dto.DictTypeRequest;
import io.rosecloud.system.service.validator.DictTypeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class DictTypeServiceImpl implements DictTypeService {

    private final DictTypeDao dictTypeDao;
    private final DictDataDao dictDataDao;
    private final DictTypeValidator dictTypeValidator;
    private final EntityCache<Long, DictType> dictTypeCache;
    private final EntityCache<String, List<DictData>> dictDataByCodeCache;
    private final EntityEventPublisher eventPublisher;

    @AuditLog(action = "dict-type-create", description = "创建字典类型")
    @Transactional
    @Override
    public Long create(DictTypeRequest request) {
        DictType dictType = DictType.of(null, request.code(), request.name(),
                request.status() == null ? 1 : request.status(), request.remark());
        dictTypeValidator.validateCreate(dictType);
        DictType saved = dictTypeDao.save(dictType);
        eventPublisher.publish(EntityChangedEvent.created(
                EntityCacheNames.DICT_TYPE, saved.getId(), null, null));
        return saved.getId();
    }

    @AuditLog(action = "dict-type-update", description = "修改字典类型")
    @Transactional
    @Override
    public void update(Long id, DictTypeRequest request) {
        DictType existing = dictTypeDao.findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.DICT_TYPE_NOT_FOUND));
        DictType updated = DictType.of(id, request.code(), request.name(),
                request.status() == null ? existing.getStatus() : request.status(), request.remark());
        dictTypeValidator.validateUpdate(updated, Optional.of(existing));
        dictTypeDao.save(updated);
        eventPublisher.publish(EntityChangedEvent.updated(
                EntityCacheNames.DICT_TYPE, id, null, null, null));
    }

    @AuditLog(action = "dict-type-delete", description = "删除字典类型")
    @Transactional
    @Override
    public void delete(Long id) {
        DictType dictType = dictTypeDao.findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.DICT_TYPE_NOT_FOUND));
        dictTypeValidator.validateDelete(dictType);
        dictDataDao.deleteByCode(dictType.getCode());
        dictTypeDao.removeById(id);
        dictDataByCodeCache.evict(dictType.getCode());
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
        return dictTypeDao.page(pageQuery,
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
        return Optional.ofNullable(dictTypeCache.getOrLoadTransactional(id, () ->
                dictTypeDao.findById(id).orElse(null)
        ));
    }
}
