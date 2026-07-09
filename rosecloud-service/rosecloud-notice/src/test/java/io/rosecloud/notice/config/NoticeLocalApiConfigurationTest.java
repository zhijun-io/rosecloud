package io.rosecloud.notice.config;

import io.rosecloud.api.notice.NoticePublishApi;
import io.rosecloud.api.notice.NoticePublishRequest;
import io.rosecloud.notice.service.NoticeService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoticeLocalApiConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(NoticeLocalApiConfiguration.class)
            .withBean(NoticeService.class, () -> mock(NoticeService.class));

    @Test
    void registersLocalAdapterWhenFeignBeanIsMissing() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(NoticePublishApi.class);

            NoticeService noticeService = ctx.getBean(NoticeService.class);
            NoticePublishApi noticePublishApi = ctx.getBean(NoticePublishApi.class);
            NoticePublishRequest request = new NoticePublishRequest("title", "content", 1,
                    "tenant-a", "role-a", "alice", 1, null, null, null, false, 1);

            when(noticeService.publish(request)).thenReturn(42L);

            assertThat(noticePublishApi.publish(request).data()).isEqualTo(42L);
            verify(noticeService).publish(request);
        });
    }
}
