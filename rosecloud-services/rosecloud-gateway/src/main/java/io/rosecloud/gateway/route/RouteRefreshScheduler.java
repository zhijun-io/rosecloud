package io.rosecloud.gateway.route;

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
 * request.
 */
@Configuration
@EnableScheduling
public class RouteRefreshScheduler {

    private final ApplicationEventPublisher publisher;

    public RouteRefreshScheduler(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        publisher.publishEvent(new RefreshRoutesEvent(this));
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 15_000)
    public void refresh() {
        publisher.publishEvent(new RefreshRoutesEvent(this));
    }
}
