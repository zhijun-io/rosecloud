package io.rosecloud.monolith;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("monolith")
@TestPropertySource(properties = {
        "spring.config.import=",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.config.import-check.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:rosecloud;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "rosecloud.jwt.secret=01234567890123456789012345678901",
        "rosecloud.audit.enabled=true",
        "rosecloud.notice.publish-check-ms=3600000"
})
class MonolithContextLoadTest {

    @Autowired
    ApplicationContext ctx;

    @Test
    void contextLoads() {
        assertThat(ctx).isNotNull();
        assertThat(ctx.getBean(io.rosecloud.api.user.SystemUserApi.class)).isNotNull();
    }
}
