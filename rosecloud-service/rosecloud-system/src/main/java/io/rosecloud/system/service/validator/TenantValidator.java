package io.rosecloud.system.service.validator;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.starter.data.validation.DataValidator;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.TenantDao;
import org.springframework.stereotype.Component;

@Component
public class TenantValidator extends DataValidator<Tenant, String> {

    private final TenantDao tenantDao;

    public TenantValidator(TenantDao tenantDao) {
        this.tenantDao = tenantDao;
    }

    @Override
    public void validateCreate(Tenant data) {
        if (tenantDao.findById(data.getId()).isPresent()) {
            throw new BizException(SystemErrorCode.TENANT_CODE_EXISTS);
        }
    }

    @Override
    protected String getId(Tenant data) {
        return data.getId();
    }
}
