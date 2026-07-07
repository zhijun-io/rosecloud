package io.rosecloud.system.service;

import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.Role;
import io.rosecloud.system.service.dto.RoleCreateRequest;

import java.util.List;

public interface RoleService {

    Long create(RoleCreateRequest request);

    PageResult<Role> page(long current, long size, String keyword);
    Role get(Long id);

    void assignMenus(Long roleId, List<Long> menuIds);

    List<Long> findMenuIdsByRoleId(Long roleId);
}
