package io.rosecloud.monolith;

import io.rosecloud.auth.RoseCloudAuthApplication;
import io.rosecloud.notice.RoseCloudNoticeApplication;
import io.rosecloud.system.RoseCloudSystemApplication;
import io.rosecloud.system.config.LocalSystemUserApi;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Monolith entry point: aggregates auth, system and notice in one process for
 * local development and small deployments. In-process wiring is selected by
 * the application name, and the gateway is bypassed so {@link MonolithJwtFilter}
 * verifies JWTs and injects identity headers.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"io.rosecloud.monolith", "io.rosecloud.auth", "io.rosecloud.system", "io.rosecloud.notice"},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {RoseCloudAuthApplication.class, RoseCloudSystemApplication.class,
                        RoseCloudNoticeApplication.class}))
@MapperScan({"io.rosecloud.system.persistence", "io.rosecloud.notice.persistence"})
@EnableScheduling
public class RoseCloudMonolithApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoseCloudMonolithApplication.class, args);
    }
}
