package io.rosecloud.notice.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables shared API Feign clients, notice mapper scanning and scheduled
 * publication tasks.
 */
@Configuration
@EnableFeignClients(basePackages = "io.rosecloud.api")
@MapperScan("io.rosecloud.notice.persistence")
@EnableScheduling
public class NoticeCoreConfiguration {
}
