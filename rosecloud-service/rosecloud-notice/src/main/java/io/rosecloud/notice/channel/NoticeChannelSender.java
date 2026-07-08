package io.rosecloud.notice.channel;

import io.rosecloud.notice.domain.NoticeChannel;

/**
 * Push-channel sender SPI. Each implementation serves one {@link NoticeChannel}
 * (e.g. email, sms); station is pull-based and has no sender. Senders are
 * invoked by {@link NoticeDispatchService} for the channels set on a notice.
 */
public interface NoticeChannelSender {

    NoticeChannel channel();

    void send(NoticeDispatchContext context);
}
