 package io.rosecloud.notice.config;
 
 import javax.sql.DataSource;
 
 import org.flywaydb.core.Flyway;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.boot.autoconfigure.AutoConfiguration;
 import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
 import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
 import org.springframework.context.annotation.Bean;
 
 /**
  * Notice-module Flyway auto-configuration.
  *
  * <p>Registers a {@link Flyway} bean scoped to {@code classpath:db/migration/notice}
  * with its own schema history table ({@code flyway_schema_history_notice}).
  * Uses {@code @Bean(initMethod = "migrate")} so migration runs eagerly on startup.
  */
 @AutoConfiguration
 @ConditionalOnClass(name = "org.flywaydb.core.Flyway")
 @ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
 public class NoticeFlywayAutoConfiguration {
 
     private static final Logger log = LoggerFactory.getLogger(NoticeFlywayAutoConfiguration.class);
 
     @Bean(initMethod = "migrate")
     public Flyway noticeFlyway(DataSource dataSource) {
         log.info("Notice Flyway: locations=classpath:db/migration/notice, table=flyway_schema_history_notice");
         return Flyway.configure()
                 .dataSource(dataSource)
                 .locations("classpath:db/migration/notice")
                 .table("flyway_schema_history_notice")
                 .baselineOnMigrate(true)
                 .validateOnMigrate(true)
                 .cleanDisabled(true)
                 .load();
     }
 }
