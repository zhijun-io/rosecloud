package io.rosecloud.notice.channel;

import io.rosecloud.api.notice.NoticeRecipient;
import io.rosecloud.api.notice.NoticeRecipientApi;
import io.rosecloud.api.notice.NoticeRecipientRequest;
import io.rosecloud.api.notice.NoticeTargetType;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.notice.domain.Notice;
import io.rosecloud.notice.domain.NoticeChannel;
import io.rosecloud.notice.domain.NoticePublishType;
import io.rosecloud.notice.domain.NoticeStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeDispatchServiceTest {

    /** Runs dispatch synchronously so assertions need no joining. */
    private static final Executor SYNC = Runnable::run;

    @Test
    void dispatchesOnlyToChannelsInMask() {
        List<NoticeRecipient> recipients = List.of(
                new NoticeRecipient(1L, "a@x.com", "110"),
                new NoticeRecipient(2L, "b@x.com", null));
        AtomicInteger emailCount = new AtomicInteger();
        AtomicInteger smsCount = new AtomicInteger();
        NoticeRecipientApi api = req -> ApiResponse.ok(recipients);
        NoticeDispatchService service = new NoticeDispatchService(api,
                List.of(capturingSender(NoticeChannel.EMAIL, emailCount),
                        capturingSender(NoticeChannel.SMS, smsCount)), SYNC);

        Notice notice = notice(NoticeChannel.EMAIL.code());
        service.doDispatch(notice, NoticeChannel.maskOf(notice.getChannels()));

        assertThat(emailCount.get()).isEqualTo(2);
        assertThat(smsCount.get()).isEqualTo(0);
    }

    @Test
    void passesTargetToRecipientResolver() {
        NoticeRecipientRequest[] captured = new NoticeRecipientRequest[1];
        NoticeRecipientApi api = req -> {
            captured[0] = req;
            return ApiResponse.ok(List.of());
        };
        NoticeDispatchService service = new NoticeDispatchService(api,
                List.of(capturingSender(NoticeChannel.EMAIL, new AtomicInteger())), SYNC);
        Notice notice = new Notice(7L, "t", "c", NoticeTargetType.TENANT.code(), 99L, "admin",
                NoticePublishType.IMMEDIATE.code(), null, null, null, NoticeStatus.PUBLISHED.code(), false,
                null, null, NoticeChannel.EMAIL.code());

        service.doDispatch(notice, NoticeChannel.maskOf(notice.getChannels()));

        assertThat(captured[0].targetType()).isEqualTo(NoticeTargetType.TENANT.code());
        assertThat(captured[0].targetTenantId()).isEqualTo(99L);
        assertThat(captured[0].targetRoleCode()).isEqualTo("admin");
    }

    private static NoticeChannelSender capturingSender(NoticeChannel channel, AtomicInteger counter) {
        return new NoticeChannelSender() {
            @Override
            public NoticeChannel channel() {
                return channel;
            }

            @Override
            public void send(Notice notice, List<NoticeRecipient> recipients) {
                counter.addAndGet(recipients.size());
            }
        };
    }

    private static Notice notice(int channels) {
        return new Notice(1L, "t", "c", NoticeTargetType.GLOBAL.code(), null, null,
                NoticePublishType.IMMEDIATE.code(), null, null, null, NoticeStatus.PUBLISHED.code(), false,
                null, null, channels);
    }
}
