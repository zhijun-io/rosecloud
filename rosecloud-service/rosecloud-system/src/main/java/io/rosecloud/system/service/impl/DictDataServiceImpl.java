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
import io.rosecloud.starter.data.PagedResults;
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.starter.data.event.EntityEventPublisher;
import io.rosecloud.system.domain.DictData;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.DictDataEntity;
import io.rosecloud.system.persistence.DictDataMapper;
import io.rosecloud.system.service.DictDataService;
import io.rosecloud.system.service.dto.DictDataRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class DictDataServiceImpl implements DictDataService {

    private final DictDataMapper dictDataMapper;
    private final EntityCache<String, List<DictData>> dictDataByCodeCache;
    private final EntityEventPublisher eventPublisher;

    @AuditLog(action = "dict-data-create", description = "创建字典项")
    @Override
    public Long create(DictDataRequest request) {
        DictDataEntity po = new DictDataEntity().toEntity(DictData.of(null, request.dictCode(), request.label(),
                request.value(), request.sort() == null ? 0 : request.sort(),
                request.status() == null ? 1 : request.status(), request.remark()));
        po.setId(null);
        dictDataMapper.insert(po);
        dictDataByCodeCache.evictAll();
        eventPublisher.publish(EntityChangedEvent.created(
                EntityCacheNames.DICT_DATA_BY_CODE, po.getId(), null, null));
        return po.getId();
    }

    @AuditLog(action = "dict-data-update", description = "修改字典项")
    @Override
    public void update(Long id, DictDataRequest request) {
        findById(id).orElseThrow(() -> new BizException(SystemErrorCode.DICT_DATA_NOT_FOUND));
        DictDataEntity po = new DictDataEntity().toEntity(DictData.of(id, request.dictCode(), request.label(), request.value(),
                request.sort() == null ? 0 : request.sort(),
                request.status() == null ? 1 : request.status(), request.remark()));
        dictDataMapper.updateById(po);
        dictDataByCodeCache.evictAll();
        eventPublisher.publish(EntityChangedEvent.updated(
                EntityCacheNames.DICT_DATA_BY_CODE, id, null, null, null));
    }

    @AuditLog(action = "dict-data-delete", description = "删除字典项")
    @Override
    public void delete(Long id) {
        dictDataMapper.deleteById(id);
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
                dictDataMapper.selectList(new LambdaQueryWrapper<DictDataEntity>()
                                .eq(DictDataEntity::getDictCode, dictCode)
                                .eq(DictDataEntity::getStatus, 1)
                                .orderByAsc(DictDataEntity::getSort))
                        .stream().map(DictDataEntity::toData).toList()
        );
    }

    @Override
    public PagedData<DictData> page(PageQuery pageQuery, String dictCode) {
        return PagedResults.page(pageQuery, DictDataEntity.class, dictDataMapper,
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
        return Optional.ofNullable(dictDataMapper.selectById(id)).map(DictDataEntity::toData);
    }

}
