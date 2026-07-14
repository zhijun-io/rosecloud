package io.rosecloud.system.service.validator;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.starter.data.validation.DataValidator;
import io.rosecloud.system.domain.SettingKey;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.SettingKeyEntity;
import io.rosecloud.system.persistence.SettingKeyMapper;
import org.springframework.stereotype.Component;

@Component
public class SettingKeyValidator extends DataValidator<SettingKey, Long> {

    private final SettingKeyMapper settingKeyMapper;

    public SettingKeyValidator(SettingKeyMapper settingKeyMapper) {
        this.settingKeyMapper = settingKeyMapper;
    }

    @Override
    public void validateCreate(SettingKey data) {
        if (settingKeyMapper.exists(new LambdaQueryWrapper<SettingKeyEntity>()
                .eq(SettingKeyEntity::getKey, data.getKey()))) {
            throw new BizException(SystemErrorCode.SETTING_KEY_EXISTS);
        }
    }

    @Override
    protected Long getId(SettingKey data) {
        return data.getId();
    }
}
