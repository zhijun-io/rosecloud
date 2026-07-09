package io.rosecloud.system.config;

import io.rosecloud.api.log.LoginLogApi;
import io.rosecloud.api.log.LoginLogRequest;
import io.rosecloud.api.notice.NoticeRecipient;
import io.rosecloud.api.notice.NoticeRecipientApi;
import io.rosecloud.api.notice.NoticeRecipientRequest;
import io.rosecloud.api.user.AuthUserInfo;
import io.rosecloud.api.user.SystemUserApi;
import io.rosecloud.api.user.SystemUserFeignApi;
import io.rosecloud.system.domain.UserRepository;
import io.rosecloud.system.service.LoginLogService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemLocalApiConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SystemLocalApiConfiguration.class)
            .withBean(LoginLogService.class, () -> mock(LoginLogService.class))
            .withBean(UserRepository.class, () -> mock(UserRepository.class));

    @Test
    void registersLocalAdaptersWhenFeignBeansAreMissing() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(LoginLogApi.class);
            assertThat(ctx).hasSingleBean(NoticeRecipientApi.class);
            assertThat(ctx).hasSingleBean(SystemUserApi.class);
            assertThat(ctx).doesNotHaveBean(SystemUserFeignApi.class);

            LoginLogService loginLogService = ctx.getBean(LoginLogService.class);
            NoticeRecipientApi noticeRecipientApi = ctx.getBean(NoticeRecipientApi.class);
            UserRepository userRepository = ctx.getBean(UserRepository.class);

            NoticeRecipientRequest recipientRequest = new NoticeRecipientRequest(1, "tenant-a", "role-a", "alice");
            List<NoticeRecipient> recipients = List.of(new NoticeRecipient(7L, "alice@example.com", "13800000000"));
            AuthUserInfo authUserInfo = new AuthUserInfo(9L, "alice", "Alice", "pwd", true, null,
                    List.of("ROLE_USER"));
            when(userRepository.findContacts(1, "tenant-a", "role-a", "alice")).thenReturn(recipients);
            when(userRepository.loadByUsername("alice")).thenReturn(java.util.Optional.of(
                    new io.rosecloud.common.security.model.SecurityUser(
                            9L, "alice", "Alice", "pwd", true, null,
                            List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")))));

            ctx.getBean(LoginLogApi.class).record(new LoginLogRequest("alice", true, null, "127.0.0.1", "ua"));
            verify(loginLogService).record(new LoginLogRequest("alice", true, null, "127.0.0.1", "ua"));

            assertThat(noticeRecipientApi.list(recipientRequest).data()).containsExactlyElementsOf(recipients);
            assertThat(ctx.getBean(SystemUserApi.class).loadUserByUsername("alice").data()).isEqualTo(authUserInfo);
        });
    }
}
