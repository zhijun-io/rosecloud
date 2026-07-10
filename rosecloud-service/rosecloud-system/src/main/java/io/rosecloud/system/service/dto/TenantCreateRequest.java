package io.rosecloud.system.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TenantCreateRequest(
        @NotBlank
        @Size(max = 10)
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9]{0,9}$")
        String tenantId,
        String name,
        String contactUser,
        String contactPhone,
        LocalDate expireTime,
        String remark,
        String tenantProfileId,
        String adminUsername) {
}
