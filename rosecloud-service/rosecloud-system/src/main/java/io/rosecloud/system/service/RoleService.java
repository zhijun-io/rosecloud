package io.rosecloud.system.service;

import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.system.domain.Role;
import io.rosecloud.system.service.dto.RoleCreateRequest;

import java.util.List;

public interface RoleService {

    Long create(RoleCreateRequest request);

    PagedData<Role> page(PageQuery pageQuery);
    Role get(Long id);

    void assignMenus(Long roleId, List<Long> menuIds);

    List<Long> findMenuIdsByRoleId(Long roleId);
}
