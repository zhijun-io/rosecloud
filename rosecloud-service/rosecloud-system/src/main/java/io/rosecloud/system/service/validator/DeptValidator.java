package io.rosecloud.system.service.validator;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.starter.data.validation.DataValidator;
import io.rosecloud.system.domain.Dept;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.DeptDao;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DeptValidator extends DataValidator<Dept, Long> {

    private final DeptDao deptDao;

    public DeptValidator(DeptDao deptDao) {
        this.deptDao = deptDao;
    }

    @Override
    public void validateCreate(Dept data) {
        validateParent(data);
    }

    @Override
    public void validateUpdate(Dept data, Optional<Dept> old) {
        validateParent(data);
    }

    private void validateParent(Dept data) {
        long parentId = data.getParentId() == null ? 0L : data.getParentId();
        if (parentId != 0L && !deptDao.existsById(parentId)) {
            throw new BizException(SystemErrorCode.DEPT_NOT_FOUND);
        }
    }

    @Override
    protected Long getId(Dept data) {
        return data.getId();
    }
}
