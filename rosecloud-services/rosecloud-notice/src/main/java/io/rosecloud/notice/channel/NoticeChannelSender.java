package io.rosecloud.notice.channel;

import io.rosecloud.api.notice.NoticeRecipient;
import io.rosecloud.notice.domain.Notice;
import io.rosecloud.notice.domain.NoticeChannel;

import java.util.List;

/**
 * Push-channel sender SPI. Each implementation serves one {@link NoticeChannel}
 * (e.g. email, sms); station is pull-based and has no sender. Senders are
 * invoked by {@link NoticeDispatchService} for the channels set on a notice.
 */
public interface NoticeChannelSender {

    NoticeChannel channel();

    void send(Notice notice, List<NoticeRecipient> recipients);
}
