package io.rosecloud.system.service.dto;

import io.rosecloud.system.domain.TenantProfileData;

public record TenantProfileUpdateRequest(String name, String description, TenantProfileData profileData) {
}
