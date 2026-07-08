package io.rosecloud.system.service.dto;

import io.rosecloud.system.domain.TenantProfileData;

public record TenantProfileCreateRequest(String id, String name, String description,
                                         TenantProfileData profileData) {
}
