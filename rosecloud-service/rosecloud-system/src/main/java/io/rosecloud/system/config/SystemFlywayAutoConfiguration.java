 package io.rosecloud.system.config;
 
 import javax.sql.DataSource;
 
 import org.flywaydb.core.Flyway;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.boot.autoconfigure.AutoConfiguration;
 import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
 import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
 import org.springframework.context.annotation.Bean;
 
 /**
  * System-module Flyway auto-configuration.
  *
  * <p>Registers a {@link Flyway} bean scoped to {@code classpath:db/migration/system}
  * with its own schema history table ({@code flyway_schema_history_system}).
  * Uses {@code @Bean(initMethod = "migrate")} so migration runs eagerly on startup.
  *
  * <p>Because this produces a {@link Flyway} bean, the default
  * {@code FlywayAutoConfiguration} is skipped via {@code @ConditionalOnMissingBean}.
  * No yaml config is needed — just having this module on the classpath is enough.
  */
 @AutoConfiguration
 @ConditionalOnClass(name = "org.flywaydb.core.Flyway")
 @ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
 public class SystemFlywayAutoConfiguration {
 
     private static final Logger log = LoggerFactory.getLogger(SystemFlywayAutoConfiguration.class);
 
     @Bean(initMethod = "migrate")
     public Flyway systemFlyway(DataSource dataSource) {
         log.info("System Flyway: locations=classpath:db/migration/system, table=flyway_schema_history_system");
         return Flyway.configure()
                 .dataSource(dataSource)
                 .locations("classpath:db/migration/system")
                 .table("flyway_schema_history_system")
                 .baselineOnMigrate(true)
                 .validateOnMigrate(true)
                 .cleanDisabled(true)
                 .load();
     }
 }
