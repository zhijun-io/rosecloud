package io.rosecloud.system.domain;

import java.time.LocalDate;

/** Domain view of a tenant. ORM-free; the persistence layer maps to/from {@code sys_tenant}. */
public record Tenant(Long id, String name, String code, TenantStatus status,
                     String contactUser, String contactPhone, LocalDate expireTime, String remark) {
}
