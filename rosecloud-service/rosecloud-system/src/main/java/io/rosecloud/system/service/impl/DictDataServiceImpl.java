package io.rosecloud.system.service.impl;

import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.SortDirection;
import io.rosecloud.common.core.model.SortField;
import io.rosecloud.common.core.event.EntityChangedEvent;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.starter.data.EntityCacheNames;
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.starter.data.event.EntityEventPublisher;
import io.rosecloud.system.domain.DictData;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.DictDataDao;
import io.rosecloud.system.persistence.DictDataEntity;
import io.rosecloud.system.service.DictDataService;
import io.rosecloud.system.service.dto.DictDataRequest;
import io.rosecloud.system.service.validator.DictDataValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class DictDataServiceImpl implements DictDataService {

    private final DictDataDao dictDataDao;
    private final DictDataValidator dictDataValidator;
    private final EntityCache<String, List<DictData>> dictDataByCodeCache;
    private final EntityEventPublisher eventPublisher;

    @AuditLog(action = "dict-data-create", description = "创建字典项")
    @Transactional
    @Override
    public Long create(DictDataRequest request) {
        DictData dictData = DictData.of(null, request.dictCode(), request.label(),
                request.value(), request.sort() == null ? 0 : request.sort(),
                request.status() == null ? 1 : request.status(), request.remark());
        dictDataValidator.validateCreate(dictData);
        DictData saved = dictDataDao.save(dictData);
        dictDataByCodeCache.evictAll();
        eventPublisher.publish(EntityChangedEvent.created(
                EntityCacheNames.DICT_DATA_BY_CODE, saved.getId(), null, null));
        return saved.getId();
    }

    @AuditLog(action = "dict-data-update", description = "修改字典项")
    @Transactional
    @Override
    public void update(Long id, DictDataRequest request) {
        DictData existing = dictDataDao.findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.DICT_DATA_NOT_FOUND));
        DictData updated = DictData.of(id, request.dictCode(), request.label(), request.value(),
                request.sort() == null ? 0 : request.sort(),
                request.status() == null ? 1 : request.status(), request.remark());
        dictDataValidator.validateUpdate(updated, Optional.of(existing));
        dictDataDao.save(updated);
        dictDataByCodeCache.evictAll();
        eventPublisher.publish(EntityChangedEvent.updated(
                EntityCacheNames.DICT_DATA_BY_CODE, id, null, null, null));
    }

    @AuditLog(action = "dict-data-delete", description = "删除字典项")
    @Transactional
    @Override
    public void delete(Long id) {
        dictDataDao.removeById(id);
        dictDataByCodeCache.evictAll();
        eventPublisher.publish(EntityChangedEvent.deleted(
                EntityCacheNames.DICT_DATA_BY_CODE, id, null, null));
    }

    @Override
    public DictData get(Long id) {
        return findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.DICT_DATA_NOT_FOUND));
    }

    @Override
    public List<DictData> listByCode(String dictCode) {
        return dictDataByCodeCache.getOrLoad(dictCode, () ->
                dictDataDao.findByCode(dictCode)
        );
    }

    @Override
    public PagedData<DictData> page(PageQuery pageQuery, String dictCode) {
        return dictDataDao.page(pageQuery,
                q -> {
                    LambdaQueryWrapper<DictDataEntity> wrapper = new LambdaQueryWrapper<>();
                    if (dictCode != null && !dictCode.isBlank()) {
                        wrapper.eq(DictDataEntity::getDictCode, dictCode);
                    }
                    return wrapper;
                },
                SortField.of("sort", SortDirection.ASC), SortField.of("createTime", SortDirection.DESC));
    }

    private Optional<DictData> findById(Long id) {
        return dictDataDao.findById(id);
    }
}
