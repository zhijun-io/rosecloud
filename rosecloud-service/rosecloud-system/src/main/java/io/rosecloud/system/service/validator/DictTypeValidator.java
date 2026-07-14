package io.rosecloud.system.service.validator;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.starter.data.validation.DataValidator;
import io.rosecloud.system.domain.DictType;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.DictTypeEntity;
import io.rosecloud.system.persistence.DictTypeMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DictTypeValidator extends DataValidator<DictType, Long> {

    private final DictTypeMapper dictTypeMapper;

    public DictTypeValidator(DictTypeMapper dictTypeMapper) {
        this.dictTypeMapper = dictTypeMapper;
    }

    @Override
    public void validateCreate(DictType data) {
        if (dictTypeMapper.exists(new LambdaQueryWrapper<DictTypeEntity>()
                .eq(DictTypeEntity::getCode, data.getCode()))) {
            throw new BizException(SystemErrorCode.DICT_TYPE_CODE_EXISTS);
        }
    }

    @Override
    public void validateUpdate(DictType data, Optional<DictType> old) {
        // Code cannot be changed via update, so no uniqueness check needed here.
        // Add future update-time constraints here.
    }

    @Override
    protected Long getId(DictType data) {
        return data.getId();
    }
}
