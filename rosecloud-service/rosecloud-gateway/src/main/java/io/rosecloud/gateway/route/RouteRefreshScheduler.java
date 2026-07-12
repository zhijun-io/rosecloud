package io.rosecloud.gateway.route;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Periodically refreshes gateway routes so newly-registered services (see
 * {@link MetadataRouteDefinitionLocator}) become routable without a restart,
 * and refreshes once on startup so dynamic routes are present from the first
 * request. A short fixed delay (15s, first at 5s) keeps local dev snappy: a
 * backend registering just after startup is routable within seconds rather
 * than half a minute.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class RouteRefreshScheduler {

    private final ApplicationEventPublisher publisher;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        publisher.publishEvent(new RefreshRoutesEvent(this));
    }

    @Scheduled(fixedDelay = 15_000, initialDelay = 5_000)
    public void refresh() {
        publisher.publishEvent(new RefreshRoutesEvent(this));
    }
}
