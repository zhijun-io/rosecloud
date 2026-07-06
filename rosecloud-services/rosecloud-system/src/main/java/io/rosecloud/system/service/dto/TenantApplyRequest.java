package io.rosecloud.system.service.dto;

import java.time.LocalDate;

public record TenantApplyRequest(String name, String code, String contactUser,
                                 String contactPhone, LocalDate expireTime, String remark) {
}
