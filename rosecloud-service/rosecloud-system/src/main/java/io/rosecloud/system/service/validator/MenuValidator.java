package io.rosecloud.system.service.validator;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.starter.data.validation.DataValidator;
import io.rosecloud.system.domain.Menu;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.MenuDao;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MenuValidator extends DataValidator<Menu, Long> {

    private final MenuDao menuDao;

    public MenuValidator(MenuDao menuDao) {
        this.menuDao = menuDao;
    }

    @Override
    public void validateCreate(Menu data) {
        validateParent(data);
    }

    @Override
    public void validateUpdate(Menu data, Optional<Menu> old) {
        validateParent(data);
    }

    private void validateParent(Menu data) {
        long parentId = data.getParentId() == null ? 0L : data.getParentId();
        if (parentId != 0L && !menuDao.existsById(parentId)) {
            throw new BizException(SystemErrorCode.MENU_NOT_FOUND);
        }
    }

    @Override
    protected Long getId(Menu data) {
        return data.getId();
    }
}
