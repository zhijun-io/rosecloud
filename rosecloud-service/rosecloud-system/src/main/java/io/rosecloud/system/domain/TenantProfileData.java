package io.rosecloud.system.domain;

import java.util.List;

/**
 * Structured tenant profile data stored as JSON inside the profile's
 * {@code additionalInfo} column.  Inspired by ThingsBoard's
 * {@code DefaultTenantProfileConfiguration}, which carries entity quotas,
 * resource limits, rate limits, TTLs, and a {@code warnThreshold}.
 *
 * <p>New fields added:
 * <ul>
 *   <li>{@code maxApiCallsPerMinute} &mdash; per-tenant API rate ceiling</li>
 *   <li>{@code maxStorageBytes} &mdash; total storage quota for file attachments</li>
 *   <li>{@code warnThresholdPercent} &mdash; percentage at which usage warnings fire</li>
 *   <li>{@code defaultStorageTtlDays} &mdash; default data-retention TTL</li>
 * </ul>
 *
 * @param packageCode       plan identifier (basic, pro, enterprise)
 * @param maxUsers          max user accounts the tenant may create
 * @param maxRoles          max roles (role definitions)
 * @param maxNoticesPerDay  daily notification send limit
 * @param maxRequestsPerMinute  general request rate limit
 * @param maxApiCallsPerMinute  API-call specific rate ceiling
 * @param maxStorageBytes   total storage quota in bytes
 * @param warnThresholdPercent  usage-warning threshold (0-100)
 * @param defaultStorageTtlDays  default data retention period in days
 * @param enabledCapabilities  feature flags (mfa, audit, sso, …)
 */
public record TenantProfileData(
        String packageCode,
        Integer maxUsers,
        Integer maxRoles,
        Integer maxNoticesPerDay,
        Integer maxRequestsPerMinute,
        Integer maxApiCallsPerMinute,
        Long maxStorageBytes,
        Integer warnThresholdPercent,
        Integer defaultStorageTtlDays,
        List<String> enabledCapabilities) {

    public TenantProfileData {
        maxApiCallsPerMinute = maxApiCallsPerMinute == null ? 60 : maxApiCallsPerMinute;
        maxStorageBytes = maxStorageBytes == null ? 0L : maxStorageBytes;
        warnThresholdPercent = warnThresholdPercent == null ? 80 : clampWarn(warnThresholdPercent);
        defaultStorageTtlDays = defaultStorageTtlDays == null ? 90 : defaultStorageTtlDays;
        enabledCapabilities = enabledCapabilities == null ? List.of() : List.copyOf(enabledCapabilities);
    }

    private static int clampWarn(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public static TenantProfileData defaults() {
        return new TenantProfileData("basic", 10, 5, 100, 60, 60, 0L, 80, 90, List.of());
    }
}
