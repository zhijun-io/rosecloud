package io.rosecloud.starter.audit;

import io.rosecloud.api.audit.AuditLogRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.util.function.Consumer;

public class AuditLogListener {
    private static final Logger log = LoggerFactory.getLogger(AuditLogListener.class);

    private final Consumer<AuditLogRequest> consumer;

    public AuditLogListener(Consumer<AuditLogRequest> consumer) {
        this.consumer = consumer;
    }

    @Async("auditLogExecutor")
    @EventListener(AuditLogRequest.class)
    public void saveLog(AuditLogRequest event) {
        try {
            consumer.accept(event);
        } catch (Throwable e) {
            log.error("保存日志失败", e);
        }
    }
}