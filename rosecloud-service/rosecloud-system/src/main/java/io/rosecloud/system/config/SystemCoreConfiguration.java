package io.rosecloud.system.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Handles system persistence mapper scanning.
 */
@Configuration
@MapperScan("io.rosecloud.system.persistence")
@EnableConfigurationProperties(UserActivationProperties.class)
@EnableScheduling
public class SystemCoreConfiguration {

}
