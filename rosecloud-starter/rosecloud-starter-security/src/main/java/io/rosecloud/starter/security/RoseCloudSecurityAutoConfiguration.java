 package io.rosecloud.starter.security;
 
 import io.rosecloud.starter.security.config.SecurityConfiguration;
 import io.rosecloud.starter.security.config.SecurityProperties;
 import org.springframework.boot.context.properties.EnableConfigurationProperties;
 import org.springframework.context.annotation.Import;
 
 @EnableConfigurationProperties(SecurityProperties.class)
 @Import(SecurityConfiguration.class)
 public class RoseCloudSecurityAutoConfiguration {
 }
