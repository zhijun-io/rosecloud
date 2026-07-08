package io.rosecloud.system.service.dto;

import java.time.LocalDate;

public record TenantApplyRequest(String name, String contactUser,
                                 String contactPhone, LocalDate expireTime, String remark,
                                 String adminUsername, String adminPassword) {
}
