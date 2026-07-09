package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.Role;
import io.rosecloud.system.domain.RoleRepository;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.RoleService;
import io.rosecloud.system.service.dto.RoleCreateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @AuditLog(action = "role-create", description = "创建角色")
    @Override
    public Long create(RoleCreateRequest request) {
        if (roleRepository.existsByCode(request.code())) {
            throw new BizException(SystemErrorCode.ROLE_CODE_EXISTS);
        }
        return roleRepository.insert(new Role(null, request.code(), request.name()));
    }

    @Override
    public PageResult<Role> page(long current, long size, String keyword) {
        return roleRepository.page(current, size, keyword);
    }
    @AuditLog(action = "role-assign-menus", description = "角色菜单授权")
    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        if (!roleRepository.existsById(roleId)) {
            throw new BizException(SystemErrorCode.ROLE_NOT_FOUND);
        }
        roleRepository.assignMenus(roleId, menuIds == null ? List.of() : menuIds);
    }

    @Override
    public List<Long> findMenuIdsByRoleId(Long roleId) {
        return roleRepository.findMenuIdsByRoleId(roleId);
    }
    @Override
    public Role get(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.ROLE_NOT_FOUND));
    }
}
