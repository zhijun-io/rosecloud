package io.rosecloud.system.service.validator;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.starter.data.validation.DataValidator;
import io.rosecloud.system.domain.DictData;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.DictTypeEntity;
import io.rosecloud.system.persistence.DictTypeMapper;
import org.springframework.stereotype.Component;

@Component
public class DictDataValidator extends DataValidator<DictData, Long> {

    private final DictTypeMapper dictTypeMapper;

    public DictDataValidator(DictTypeMapper dictTypeMapper) {
        this.dictTypeMapper = dictTypeMapper;
    }

    @Override
    public void validateCreate(DictData data) {
        if (data.getDictCode() != null && !dictTypeMapper.exists(new LambdaQueryWrapper<DictTypeEntity>()
                .eq(DictTypeEntity::getCode, data.getDictCode()))) {
            throw new BizException(SystemErrorCode.DICT_TYPE_NOT_FOUND);
        }
    }

    @Override
    protected Long getId(DictData data) {
        return data.getId();
    }
}
