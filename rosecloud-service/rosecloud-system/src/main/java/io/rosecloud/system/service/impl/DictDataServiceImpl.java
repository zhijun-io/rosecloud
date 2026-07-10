package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.DictData;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.DictDataEntity;
import io.rosecloud.system.persistence.DictDataMapper;
import io.rosecloud.system.service.DictDataService;
import io.rosecloud.system.service.dto.DictDataRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DictDataServiceImpl implements DictDataService {

    private final DictDataMapper dictDataMapper;

    public DictDataServiceImpl(DictDataMapper dictDataMapper) {
        this.dictDataMapper = dictDataMapper;
    }

    @AuditLog(action = "dict-data-create", description = "创建字典项")
    @Override
    public Long create(DictDataRequest request) {
        DictDataEntity po = toEntity(new DictData(null, request.dictCode(), request.label(),
                request.value(), request.sort() == null ? 0 : request.sort(),
                request.status() == null ? 1 : request.status(), request.remark()));
        po.setId(null);
        dictDataMapper.insert(po);
        return po.getId();
    }

    @AuditLog(action = "dict-data-update", description = "修改字典项")
    @Override
    public void update(Long id, DictDataRequest request) {
        findById(id).orElseThrow(() -> new BizException(SystemErrorCode.DICT_DATA_NOT_FOUND));
        DictDataEntity po = toEntity(new DictData(id, request.dictCode(), request.label(), request.value(),
                request.sort() == null ? 0 : request.sort(),
                request.status() == null ? 1 : request.status(), request.remark()));
        dictDataMapper.updateById(po);
    }

    @AuditLog(action = "dict-data-delete", description = "删除字典项")
    @Override
    public void delete(Long id) {
        dictDataMapper.deleteById(id);
    }

    @Override
    public DictData get(Long id) {
        return findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.DICT_DATA_NOT_FOUND));
    }

    @Override
    public List<DictData> listByCode(String dictCode) {
        return dictDataMapper.selectList(new LambdaQueryWrapper<DictDataEntity>()
                        .eq(DictDataEntity::getDictCode, dictCode)
                        .eq(DictDataEntity::getStatus, 1)
                        .orderByAsc(DictDataEntity::getSort)).stream().map(this::toDomain).toList();
    }

    @Override
    public PageResult<DictData> page(long current, long size, String dictCode) {
        Page<DictDataEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<DictDataEntity> wrapper = new LambdaQueryWrapper<>();
        if (dictCode != null && !dictCode.isBlank()) {
            wrapper.eq(DictDataEntity::getDictCode, dictCode);
        }
        wrapper.orderByAsc(DictDataEntity::getSort).orderByDesc(DictDataEntity::getCreateTime);
        IPage<DictDataEntity> result = dictDataMapper.selectPage(page, wrapper);
        List<DictData> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private Optional<DictData> findById(Long id) {
        return Optional.ofNullable(dictDataMapper.selectById(id)).map(this::toDomain);
    }

    private DictData toDomain(DictDataEntity po) {
        return new DictData(po.getId(), po.getDictCode(), po.getLabel(), po.getValue(),
                po.getSort(), po.getStatus(), po.getRemark(), po.getCreateTime(), po.getCreateBy(),
                po.getUpdateTime(), po.getUpdateBy());
    }

    private DictDataEntity toEntity(DictData d) {
        DictDataEntity po = new DictDataEntity();
        po.setId(d.getId());
        po.setDictCode(d.getDictCode());
        po.setLabel(d.getLabel());
        po.setValue(d.getValue());
        po.setSort(d.getSort());
        po.setStatus(d.getStatus());
        po.setRemark(d.getRemark());
        po.setCreateTime(d.getCreateTime());
        po.setCreateBy(d.getCreateBy());
        po.setUpdateTime(d.getUpdateTime());
        po.setUpdateBy(d.getUpdateBy());
        return po;
    }
}
