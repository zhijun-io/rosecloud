package io.rosecloud.system.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Enables shared API Feign clients and system persistence mapper scanning.
 */
@Configuration
@EnableFeignClients(basePackages = "io.rosecloud.api")
@MapperScan("io.rosecloud.system.persistence")
public class SystemCoreConfiguration {
}
