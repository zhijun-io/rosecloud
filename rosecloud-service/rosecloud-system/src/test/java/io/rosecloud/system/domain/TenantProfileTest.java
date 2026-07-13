package io.rosecloud.system.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantProfileTest {

    @Test
    void storesTypedProfileAsJsonBytes() {
        TenantProfile profile = new TenantProfile("profile-1", "Pro", "Production tier",
                new TenantProfileData("pro", 50, 20, 500, 120, 60, 0L, 80, 90, java.util.List.of("mfa", "audit")));

        assertThat(profile.getId()).isEqualTo("profile-1");
        assertThat(profile.getName()).isEqualTo("Pro");
        assertThat(profile.getDescription()).isEqualTo("Production tier");
        assertThat(profile.getProfileData()).isEqualTo(
                new TenantProfileData("pro", 50, 20, 500, 120, 60, 0L, 80, 90, java.util.List.of("mfa", "audit")));
        assertThat(profile.getAdditionalInfo()).isEqualTo(new ObjectMapper().valueToTree(
                new TenantProfileData("pro", 50, 20, 500, 120, 60, 0L, 80, 90, java.util.List.of("mfa", "audit"))));
    }

    @Test
    void readsTypedProfileFromJsonBytes() throws Exception {
        TenantProfile profile = new TenantProfile("profile-2", "Enterprise", "High-capacity tier",
                new ObjectMapper().readTree("""
                        {"packageCode":"enterprise","maxUsers":200,"maxRoles":50,"maxNoticesPerDay":2000,"maxRequestsPerMinute":240,"enabledCapabilities":["mfa"]}
                        """));

        assertThat(profile.getProfileData()).isEqualTo(
                new TenantProfileData("enterprise", 200, 50, 2_000, 240, 60, 0L, 80, 90, java.util.List.of("mfa")));
    }

    @Test
    void fallsBackToDefaultsWhenDataIsMissing() {
        TenantProfile profile = new TenantProfile("profile-3", "Basic", "Default tier", (com.fasterxml.jackson.databind.JsonNode) null);

        assertThat(profile.getProfileData()).isEqualTo(TenantProfileData.defaults());
        assertThat(profile.getAdditionalInfo()).isEqualTo(new ObjectMapper().valueToTree(TenantProfileData.defaults()));
    }
}
