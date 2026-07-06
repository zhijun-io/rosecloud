package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.DictData;
import io.rosecloud.system.domain.DictDataRepository;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.DictDataService;
import io.rosecloud.system.service.dto.DictDataRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DictDataServiceImpl implements DictDataService {

    private final DictDataRepository dictDataRepository;

    public DictDataServiceImpl(DictDataRepository dictDataRepository) {
        this.dictDataRepository = dictDataRepository;
    }

    @AuditLog(action = "dict-data-create", description = "创建字典项")
    @Override
    public Long create(DictDataRequest request) {
        return dictDataRepository.insert(new DictData(null, request.dictCode(), request.label(),
                request.value(), request.sort() == null ? 0 : request.sort(),
                request.status() == null ? 1 : request.status(), request.remark()));
    }

    @AuditLog(action = "dict-data-update", description = "修改字典项")
    @Override
    public void update(Long id, DictDataRequest request) {
        dictDataRepository.findById(id).orElseThrow(() -> new BizException(SystemErrorCode.DICT_DATA_NOT_FOUND));
        dictDataRepository.update(new DictData(id, request.dictCode(), request.label(), request.value(),
                request.sort() == null ? 0 : request.sort(),
                request.status() == null ? 1 : request.status(), request.remark()));
    }

    @AuditLog(action = "dict-data-delete", description = "删除字典项")
    @Override
    public void delete(Long id) {
        dictDataRepository.deleteById(id);
    }

    @Override
    public DictData get(Long id) {
        return dictDataRepository.findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.DICT_DATA_NOT_FOUND));
    }

    @Override
    public List<DictData> listByCode(String dictCode) {
        return dictDataRepository.findEnabledByDictCode(dictCode);
    }

    @Override
    public PageResult<DictData> page(long current, long size, String dictCode) {
        return dictDataRepository.page(current, size, dictCode);
    }
}
