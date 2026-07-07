package io.rosecloud.system.service.dto;

import java.util.List;

public record RoleMenuAssignRequest(List<Long> menuIds) {
}
