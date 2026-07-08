package io.rosecloud.system.domain;

import java.util.List;

/** Structured tenant profile data stored as JSON. */
public record TenantProfileData(String packageCode, Integer maxUsers, Integer maxRoles,
                                Integer maxNoticesPerDay, Integer maxRequestsPerMinute,
                                List<String> enabledCapabilities) {

    public TenantProfileData {
        enabledCapabilities = enabledCapabilities == null ? List.of() : List.copyOf(enabledCapabilities);
    }

    public static TenantProfileData defaults() {
        return new TenantProfileData("basic", 10, 5, 100, 60, List.of());
    }
}
