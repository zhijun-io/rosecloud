package io.rosecloud.notice.channel;

import io.rosecloud.notice.domain.Notice;
import io.rosecloud.notice.domain.NoticeChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * Dispatches a notice's push channels (email/sms) using the recipient snapshot
 * carried with the notice. Station is pull-based and needs no dispatch. Runs
 * asynchronously so publishing is not blocked by outbound delivery; per-recipient
 * failures are logged, never propagated.
 */
@Component
public class NoticeDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NoticeDispatchService.class);

    private final Map<NoticeChannel, NoticeChannelSender> senders;
    private final Executor executor;

    public NoticeDispatchService(List<NoticeChannelSender> senders,
                                 @Qualifier("noticeDispatchExecutor") Executor executor) {
        this.senders = senders.stream().collect(toMap(NoticeChannelSender::channel, identity()));
        this.executor = executor;
    }

    public void dispatch(Notice notice) {
        int mask = NoticeChannel.maskOf(notice.getChannels());
        if (!NoticeChannel.EMAIL.in(mask)) {
            return;
        }
        CompletableFuture.runAsync(() -> doDispatch(notice, mask), executor)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("notice {} dispatch future terminated exceptionally", notice.getId(), ex);
                    }
                });
    }

    void doDispatch(Notice notice, int mask) {
        try {
            if (notice.getRecipients() == null || notice.getRecipients().isEmpty()) {
                // No recipient snapshot: only explicit-recipient (USER) notices carry contacts
                // today. ROLE/TENANT/GLOBAL targets require recipient resolution at dispatch
                // time (not yet wired), so push channels have nobody to deliver to.
                if (NoticeChannel.EMAIL.in(mask)) {
                    log.warn("notice {} has email channel but no resolved recipients; "
                            + "push delivery skipped (recipient resolution not implemented)", notice.getId());
                }
                return;
            }
            // SMS has no sender yet (the SmsNoticeSender stub was removed); email is the only
            // wired push channel. Iterate EMAIL only — re-add SMS here once a real provider lands.
            for (NoticeChannel channel : new NoticeChannel[]{NoticeChannel.EMAIL}) {
                if (!channel.in(mask)) {
                    continue;
                }
                NoticeChannelSender sender = senders.get(channel);
                if (sender != null) {
                    sender.send(new NoticeDispatchContext(notice, channel, notice.getRecipients()));
                } else {
                    log.warn("no sender registered for channel {} on notice {}", channel, notice.getId());
                }
            }
        } catch (Exception e) {
            log.error("failed to dispatch notice {}", notice.getId(), e);
        }
    }
}
