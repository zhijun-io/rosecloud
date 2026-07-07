package io.rosecloud.system.service.dto;

import java.util.List;

public record UserRoleAssignRequest(List<Long> roleIds) {
}
