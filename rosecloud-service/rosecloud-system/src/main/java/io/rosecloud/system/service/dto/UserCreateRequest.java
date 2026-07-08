package io.rosecloud.system.service.dto;

public record UserCreateRequest(String username, String password, String nickname, String tenantId) {
}
