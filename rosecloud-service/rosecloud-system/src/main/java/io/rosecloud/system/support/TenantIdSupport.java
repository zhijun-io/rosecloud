package io.rosecloud.system.support;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import io.rosecloud.system.error.SystemErrorCode;

import java.util.Locale;
import java.util.regex.Pattern;

/** Canonical tenant id rules for system-owned tenant records and requests. */
public final class TenantIdSupport {

    private static final Pattern TENANT_ID_PATTERN = Pattern.compile("^[A-Z][A-Z0-9]{0,9}$");

    private TenantIdSupport() {
    }

    public static String normalize(String tenantId) {
        if (tenantId == null) {
            return null;
        }
        return tenantId.trim().toUpperCase(Locale.ROOT);
    }

    public static String requireValid(String tenantId) {
        String normalized = normalize(tenantId);
        if (normalized == null || normalized.isBlank() || !TENANT_ID_PATTERN.matcher(normalized).matches()) {
            throw new BizException(SystemErrorCode.TENANT_ID_INVALID);
        }
        return normalized;
    }

    public static String requireCreatable(String tenantId) {
        String normalized = requireValid(tenantId);
        if (TenantContextHolder.SYSTEM_TENANT_ID.equals(normalized)) {
            throw new BizException(SystemErrorCode.TENANT_ID_RESERVED);
        }
        return normalized;
    }
}
