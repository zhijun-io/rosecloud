package io.rosecloud.system.service;

import io.rosecloud.system.domain.Menu;
import io.rosecloud.system.service.dto.MenuRequest;
import io.rosecloud.system.service.dto.MenuTreeNode;
import io.rosecloud.system.service.dto.UserMenuResult;

import java.util.List;

public interface MenuService {

    Long create(MenuRequest request);

    void update(Long id, MenuRequest request);

    void delete(Long id);

    List<Menu> list();

    List<MenuTreeNode> tree();

    UserMenuResult myMenus();
}
