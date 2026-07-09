package io.rosecloud.system.config;

import io.rosecloud.api.log.LoginLogApi;
import io.rosecloud.api.user.SystemUserApi;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.system.domain.UserRepository;
import io.rosecloud.system.service.LoginLogService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Local API adapters used when no Feign client bean is present.
 */
@Configuration
public class SystemLocalApiConfiguration {

    @Bean
    @ConditionalOnMissingBean(LoginLogApi.class)
    public LoginLogApi loginLogApi(LoginLogService loginLogService) {
        return request -> loginLogService.record(request);
    }

    @Bean
    @ConditionalOnMissingBean(SystemUserApi.class)
    public SystemUserApi systemUserApi(UserRepository userRepository) {
        return username -> ApiResponse.ok(userRepository.loadByUsername(username).orElse(null));
    }
}
