package io.rosecloud.monolith;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers
@TestPropertySource(properties = {
        "spring.config.import=optional:classpath:rosecloud-common.yaml",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.config.import-check.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "rosecloud.security.jwt.secret=wICxDnixZdbwEvLEPfL1ZR5SNVZnEouFKjQ/Mi5pMAkO+9zOA9eAiVf82sZ+xZpKgoy322rJWXKpcPy8UsALlQ==",
        "rosecloud.security.internal-token=UIW0QRgE96atD9YB1z4Xo9Dlar5fECsp",
        "rosecloud.audit.enabled=true",
        "rosecloud.notice.publish-check-ms=3600000"
})
class MonolithContextLoadTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("rosecloud")
            .withUsername("rosecloud")
            .withPassword("rosecloud123");

    @DynamicPropertySource
    static void registerMysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    ApplicationContext ctx;

    @Test
    void contextLoads() {
        assertThat(ctx).isNotNull();
        assertThat(ctx.getBean(io.rosecloud.system.service.UserService.class)).isNotNull();
        assertThat(ctx.getBean(io.rosecloud.api.notice.NoticePublishApi.class)).isNotNull();
        assertThat(ctx.getBean(TenantLineInnerInterceptor.class)).isNotNull();
    }
}
