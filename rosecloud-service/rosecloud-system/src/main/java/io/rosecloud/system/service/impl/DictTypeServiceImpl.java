package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.DictDataRepository;
import io.rosecloud.system.domain.DictType;
import io.rosecloud.system.domain.DictTypeRepository;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.DictTypeService;
import io.rosecloud.system.service.dto.DictTypeRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DictTypeServiceImpl implements DictTypeService {

    private final DictTypeRepository dictTypeRepository;
    private final DictDataRepository dictDataRepository;

    public DictTypeServiceImpl(DictTypeRepository dictTypeRepository, DictDataRepository dictDataRepository) {
        this.dictTypeRepository = dictTypeRepository;
        this.dictDataRepository = dictDataRepository;
    }

    @AuditLog(action = "dict-type-create", description = "创建字典类型")
    @Override
    public Long create(DictTypeRequest request) {
        if (dictTypeRepository.existsByCode(request.code())) {
            throw new BizException(SystemErrorCode.DICT_TYPE_CODE_EXISTS);
        }
        return dictTypeRepository.insert(new DictType(null, request.code(), request.name(),
                request.status() == null ? 1 : request.status(), request.remark()));
    }

    @AuditLog(action = "dict-type-update", description = "修改字典类型")
    @Override
    public void update(Long id, DictTypeRequest request) {
        DictType existing = dictTypeRepository.findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.DICT_TYPE_NOT_FOUND));
        dictTypeRepository.update(new DictType(id, request.code(), request.name(),
                request.status() == null ? existing.getStatus() : request.status(), request.remark()));
    }

    @AuditLog(action = "dict-type-delete", description = "删除字典类型")
    @Transactional
    @Override
    public void delete(Long id) {
        DictType dictType = dictTypeRepository.findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.DICT_TYPE_NOT_FOUND));
        dictDataRepository.deleteByDictCode(dictType.getCode());
        dictTypeRepository.deleteById(id);
    }

    @Override
    public DictType get(Long id) {
        return dictTypeRepository.findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.DICT_TYPE_NOT_FOUND));
    }

    @Override
    public PageResult<DictType> page(long current, long size, String keyword) {
        return dictTypeRepository.page(current, size, keyword);
    }
}
