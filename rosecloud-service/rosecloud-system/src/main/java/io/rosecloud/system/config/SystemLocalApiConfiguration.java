package io.rosecloud.system.config;

import io.rosecloud.api.log.LoginLogApi;
import io.rosecloud.api.log.LoginLogRequest;
import io.rosecloud.api.notice.NoticeRecipient;
import io.rosecloud.api.notice.NoticeRecipientApi;
import io.rosecloud.api.notice.NoticeRecipientRequest;
import io.rosecloud.api.user.SystemUserApi;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.system.domain.UserRepository;
import io.rosecloud.system.service.LoginLogService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

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
    @ConditionalOnMissingBean(NoticeRecipientApi.class)
    public NoticeRecipientApi noticeRecipientApi(UserRepository userRepository) {
        return request -> ApiResponse.ok(userRepository.findContacts(
                request.targetType(), request.targetTenantId(), request.targetRoleCode(), request.targetUsername()));
    }

    @Bean
    @ConditionalOnMissingBean(SystemUserApi.class)
    public SystemUserApi systemUserApi(UserRepository userRepository) {
        return new SystemUserApi() {
            @Override
            public ApiResponse<SecurityUser> loadUserByUsername(String username) {
                return ApiResponse.ok(userRepository.loadByUsername(username).orElse(null));
            }

            @Override
            public ApiResponse<Void> updateLastLoginTime(Long userId, java.time.LocalDateTime lastLoginTime) {
                userRepository.updateLastLoginTime(userId, lastLoginTime);
                return ApiResponse.ok();
            }
        };
    }
}
