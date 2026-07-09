package io.rosecloud.monolith.config;

import io.rosecloud.auth.RoseCloudAuthApplication;
import io.rosecloud.notice.RoseCloudNoticeApplication;
import io.rosecloud.system.RoseCloudSystemApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Aggregates the auth, system and notice modules into the monolith runtime.
 */
@Configuration
@ComponentScan(basePackages = {"io.rosecloud.monolith", "io.rosecloud.auth", "io.rosecloud.system", "io.rosecloud.notice"},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {RoseCloudAuthApplication.class, RoseCloudSystemApplication.class,
                        RoseCloudNoticeApplication.class}))
public class MonolithInfrastructureConfiguration {
}
