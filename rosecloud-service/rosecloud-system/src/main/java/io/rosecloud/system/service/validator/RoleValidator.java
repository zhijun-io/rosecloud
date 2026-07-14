package io.rosecloud.system.service.validator;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.starter.data.validation.DataValidator;
import io.rosecloud.system.domain.Role;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.RoleEntity;
import io.rosecloud.system.persistence.RoleMapper;
import org.springframework.stereotype.Component;

@Component
public class RoleValidator extends DataValidator<Role, Long> {

    private final RoleMapper roleMapper;

    public RoleValidator(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    @Override
    public void validateCreate(Role data) {
        if (roleMapper.exists(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getCode, data.getCode()))) {
            throw new BizException(SystemErrorCode.ROLE_CODE_EXISTS);
        }
    }

    @Override
    protected Long getId(Role data) {
        return data.getId();
    }
}
