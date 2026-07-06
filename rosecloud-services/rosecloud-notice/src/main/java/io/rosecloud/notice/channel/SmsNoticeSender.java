package io.rosecloud.notice.channel;

import io.rosecloud.api.notice.NoticeRecipient;
import io.rosecloud.notice.domain.Notice;
import io.rosecloud.notice.domain.NoticeChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SMS sender stub: logs intended sends. Real SMS delivery (provider client) is
 * a follow-up; wire a concrete sender here once a provider is chosen.
 */
@Component
public class SmsNoticeSender implements NoticeChannelSender {

    private static final Logger log = LoggerFactory.getLogger(SmsNoticeSender.class);

    @Override
    public NoticeChannel channel() {
        return NoticeChannel.SMS;
    }

    @Override
    public void send(Notice notice, List<NoticeRecipient> recipients) {
        int withPhone = (int) recipients.stream().filter(r -> r.phone() != null && !r.phone().isBlank()).count();
        log.info("sms dispatch (stub): notice={} title={} recipients_with_phone={}",
                notice.id(), notice.title(), withPhone);
    }
}
