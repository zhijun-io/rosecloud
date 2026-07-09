package io.rosecloud.system.config;

import io.rosecloud.api.log.LoginLogApi;
import io.rosecloud.api.notice.NoticeRecipientApi;
import io.rosecloud.api.user.SystemUserApi;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.system.domain.UserRepository;
import io.rosecloud.system.service.LoginLogService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Local API adapters for the monolith profile. The internal API contracts
 * still resolve, but calls stay in-process instead of creating HTTP clients.
 */
@Configuration
@Profile("monolith")
public class SystemLocalApiConfiguration {

    @Bean
    public LoginLogApi loginLogApi(LoginLogService loginLogService) {
        return request -> {
            loginLogService.record(request);
            return ApiResponse.ok();
        };
    }

    @Bean
    public NoticeRecipientApi noticeRecipientApi(UserRepository userRepository) {
        return request -> ApiResponse.ok(userRepository.findContacts(
                request.targetType(), request.targetTenantId(), request.targetRoleCode(), request.targetUsername()));
    }

    @Bean
    public SystemUserApi systemUserApi(UserRepository userRepository) {
        return new SystemUserApi() {
            @Override
            public ApiResponse<io.rosecloud.common.security.model.SecurityUser> loadUserByUsername(String username) {
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
