package io.rosecloud.notice.channel;

import io.rosecloud.api.notice.NoticeRecipient;
import io.rosecloud.api.notice.NoticeRecipientApi;
import io.rosecloud.api.notice.NoticeRecipientRequest;
import io.rosecloud.notice.domain.Notice;
import io.rosecloud.notice.domain.NoticeChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * Resolves recipients for a notice's push channels (email/sms) and dispatches
 * to the registered {@link NoticeChannelSender}s. Station is pull-based and
 * needs no dispatch. Runs asynchronously so publishing is not blocked by
 * outbound delivery; per-recipient failures are logged, never propagated.
 */
@Component
public class NoticeDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NoticeDispatchService.class);

    private final NoticeRecipientApi recipientApi;
    private final Map<NoticeChannel, NoticeChannelSender> senders;

    public NoticeDispatchService(NoticeRecipientApi recipientApi, List<NoticeChannelSender> senders) {
        this.recipientApi = recipientApi;
        this.senders = senders.stream().collect(toMap(NoticeChannelSender::channel, identity()));
    }

    public void dispatch(Notice notice) {
        int mask = NoticeChannel.maskOf(notice.channels());
        if (!NoticeChannel.EMAIL.in(mask) && !NoticeChannel.SMS.in(mask)) {
            return;
        }
        CompletableFuture.runAsync(() -> doDispatch(notice, mask));
    }

    void doDispatch(Notice notice, int mask) {
        try {
            List<NoticeRecipient> recipients = recipientApi.list(new NoticeRecipientRequest(
                    notice.targetType(), notice.targetTenantId(), notice.targetRoleCode())).data();
            if (recipients == null || recipients.isEmpty()) {
                return;
            }
            for (NoticeChannel channel : new NoticeChannel[]{NoticeChannel.EMAIL, NoticeChannel.SMS}) {
                if (!channel.in(mask)) {
                    continue;
                }
                NoticeChannelSender sender = senders.get(channel);
                if (sender != null) {
                    sender.send(notice, recipients);
                } else {
                    log.warn("no sender registered for channel {} on notice {}", channel, notice.id());
                }
            }
        } catch (Exception e) {
            log.warn("failed to dispatch notice {}", notice.id(), e);
        }
    }
}
