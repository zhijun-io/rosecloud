package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.starter.audit.AuditLog;
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

@Service
public class DictTypeServiceImpl implements DictTypeService {

    private final DictTypeMapper dictTypeMapper;
    private final DictDataMapper dictDataMapper;

    public DictTypeServiceImpl(DictTypeMapper dictTypeMapper, DictDataMapper dictDataMapper) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictDataMapper = dictDataMapper;
    }

    @AuditLog(action = "dict-type-create", description = "创建字典类型")
    @Override
    public Long create(DictTypeRequest request) {
        if (dictTypeMapper.exists(new LambdaQueryWrapper<DictTypeEntity>().eq(DictTypeEntity::getCode, request.code()))) {
            throw new BizException(SystemErrorCode.DICT_TYPE_CODE_EXISTS);
        }
        DictTypeEntity po = toEntity(new DictType(null, request.code(), request.name(),
                request.status() == null ? 1 : request.status(), request.remark()));
        po.setId(null);
        dictTypeMapper.insert(po);
        return po.getId();
    }

    @AuditLog(action = "dict-type-update", description = "修改字典类型")
    @Override
    public void update(Long id, DictTypeRequest request) {
        DictType existing = findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.DICT_TYPE_NOT_FOUND));
        DictTypeEntity po = toEntity(new DictType(id, request.code(), request.name(),
                request.status() == null ? existing.getStatus() : request.status(), request.remark()));
        dictTypeMapper.updateById(po);
    }

    @AuditLog(action = "dict-type-delete", description = "删除字典类型")
    @Transactional
    @Override
    public void delete(Long id) {
        DictType dictType = findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.DICT_TYPE_NOT_FOUND));
        dictDataMapper.delete(new LambdaQueryWrapper<DictDataEntity>().eq(DictDataEntity::getDictCode, dictType.getCode()));
        dictTypeMapper.deleteById(id);
    }

    @Override
    public DictType get(Long id) {
        return findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.DICT_TYPE_NOT_FOUND));
    }

    @Override
    public PageResult<DictType> page(long current, long size, String keyword) {
        Page<DictTypeEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<DictTypeEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(DictTypeEntity::getCode, keyword).or().like(DictTypeEntity::getName, keyword);
        }
        wrapper.orderByDesc(DictTypeEntity::getCreateTime);
        IPage<DictTypeEntity> result = dictTypeMapper.selectPage(page, wrapper);
        List<DictType> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private Optional<DictType> findById(Long id) {
        return Optional.ofNullable(dictTypeMapper.selectById(id)).map(this::toDomain);
    }

    private DictType toDomain(DictTypeEntity po) {
        return new DictType(po.getId(), po.getCode(), po.getName(), po.getStatus(), po.getRemark(),
                po.getCreateTime(), po.getCreateBy(), po.getUpdateTime(), po.getUpdateBy());
    }

    private DictTypeEntity toEntity(DictType d) {
        DictTypeEntity po = new DictTypeEntity();
        po.setId(d.getId());
        po.setCode(d.getCode());
        po.setName(d.getName());
        po.setStatus(d.getStatus());
        po.setRemark(d.getRemark());
        po.setCreateTime(d.getCreateTime());
        po.setCreateBy(d.getCreateBy());
        po.setUpdateTime(d.getUpdateTime());
        po.setUpdateBy(d.getUpdateBy());
        return po;
    }
}
