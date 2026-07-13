package io.rosecloud.starter.tenant.core;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;

/** Configuration for {@code rosecloud.tenant.*}. */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "rosecloud.tenant")
public class TenantProperties {

    /** Isolation strategy. COLUMN by default. */
    @NotNull
    private MultiTenantType type = MultiTenantType.COLUMN;

    /** Tables ignored by row-level isolation. */
    private List<String> ignoreTables = Collections.emptyList();
}
