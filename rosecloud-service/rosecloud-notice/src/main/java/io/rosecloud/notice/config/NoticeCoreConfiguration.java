package io.rosecloud.notice.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Handles notice mapper scanning and scheduled publication tasks.
 */
@Configuration
@MapperScan("io.rosecloud.notice.persistence")
@EnableScheduling
public class NoticeCoreConfiguration {
}
