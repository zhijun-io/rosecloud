package io.rosecloud.system.service.dto;

import java.time.LocalDate;

public record TenantCreateRequest(String name, String contactUser,
                                  String contactPhone, LocalDate expireTime, String remark,
                                  String tenantProfileId, String adminUsername) {
}
