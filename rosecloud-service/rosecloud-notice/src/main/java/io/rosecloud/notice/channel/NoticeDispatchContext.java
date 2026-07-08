package io.rosecloud.notice.channel;

import io.rosecloud.api.notice.NoticeRecipient;
import io.rosecloud.notice.domain.Notice;
import io.rosecloud.notice.domain.NoticeChannel;

import java.util.List;

/** Immutable context for dispatching one notice to one channel. */
public record NoticeDispatchContext(Notice notice, NoticeChannel channel, List<NoticeRecipient> recipients) {

    public NoticeDispatchContext {
        recipients = recipients == null ? List.of() : List.copyOf(recipients);
    }
}
