package io.rosecloud.notice.channel;

import io.rosecloud.api.notice.NoticeRecipient;
import io.rosecloud.api.notice.NoticeTargetType;
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
        NoticeDispatchService service = new NoticeDispatchService(
                List.of(capturingSender(NoticeChannel.EMAIL, emailCount)), SYNC);

        Notice notice = notice(NoticeChannel.EMAIL.code(), recipients);
        service.doDispatch(notice, NoticeChannel.maskOf(notice.getChannels()));

        assertThat(emailCount.get()).isEqualTo(2);
    }

    @Test
    void skipsDispatchWhenNoRecipientsWerePersisted() {
        AtomicInteger emailCount = new AtomicInteger();
        NoticeDispatchService service = new NoticeDispatchService(
                List.of(capturingSender(NoticeChannel.EMAIL, emailCount)), SYNC);
        Notice notice = notice(NoticeChannel.EMAIL.code(), List.of());

        service.doDispatch(notice, NoticeChannel.maskOf(notice.getChannels()));

        assertThat(emailCount.get()).isEqualTo(0);
    }

    private static NoticeChannelSender capturingSender(NoticeChannel channel, AtomicInteger counter) {
        return new NoticeChannelSender() {
            @Override
            public NoticeChannel channel() {
                return channel;
            }

            @Override
            public void send(NoticeDispatchContext context) {
                counter.addAndGet(context.recipients().size());
            }
        };
    }

    private static Notice notice(int channels, List<NoticeRecipient> recipients) {
        return new Notice(1L, "t", "c", NoticeTargetType.GLOBAL.code(), null, null,
                null, NoticePublishType.IMMEDIATE.code(), null, null, null, NoticeStatus.PUBLISHED.code(), false,
                null, null, channels, recipients, null, null, null, null);
    }
}
