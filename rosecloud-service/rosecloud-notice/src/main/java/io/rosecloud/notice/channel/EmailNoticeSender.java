package io.rosecloud.notice.channel;

import io.rosecloud.api.notice.NoticeRecipient;
import io.rosecloud.notice.domain.Notice;
import io.rosecloud.notice.domain.NoticeChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Email sender. Only loaded when {@code spring-boot-starter-mail} is on the
 * classpath; the {@link JavaMailSender} itself is resolved lazily so the notice
 * service still starts without mail configured (the channel is skipped).
 */
@Component
@ConditionalOnClass(JavaMailSender.class)
public class EmailNoticeSender implements NoticeChannelSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNoticeSender.class);

    private final ObjectProvider<JavaMailSender> mailSender;

    public EmailNoticeSender(ObjectProvider<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public NoticeChannel channel() {
        return NoticeChannel.EMAIL;
    }

    @Override
    public void send(Notice notice, List<NoticeRecipient> recipients) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("email channel requested but no JavaMailSender configured; skipping notice {}", notice.getId());
            return;
        }
        for (NoticeRecipient r : recipients) {
            if (r.email() == null || r.email().isBlank()) {
                continue;
            }
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(r.email());
                message.setSubject(notice.getTitle());
                message.setText(notice.getContent());
                sender.send(message);
            } catch (Exception e) {
                log.warn("failed to email notice {} to {}", notice.getId(), r.email(), e);
            }
        }
    }
}
