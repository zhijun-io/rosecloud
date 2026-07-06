package io.rosecloud.starter.storage;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;

/**
 * Registers a {@link FileStorage}. Activated by
 * {@code rosecloud.storage.enabled=true}; the local filesystem backend is
 * provided by default ({@code rosecloud.storage.type=local}). Consumers needing
 * object storage (S3/OSS) define their own {@link FileStorage} bean, which takes
 * precedence via {@code @ConditionalOnMissingBean}.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "rosecloud.storage", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(StorageProperties.class)
public class StorageAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "rosecloud.storage", name = "type", havingValue = "local", matchIfMissing = true)
    @ConditionalOnMissingBean
    public FileStorage localFileStorage(StorageProperties properties) {
        return new LocalFileStorage(Path.of(properties.getBaseDir()));
    }
}
