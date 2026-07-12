package io.rosecloud.gateway.route;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

/**
 * Builds gateway routes from service-discovery metadata: a registered service
 * that declares {@value #PATH_META} metadata gets a {@code lb://} route
 * automatically, so adding a service needs no change in the gateway — only a
 * metadata entry in its own config. Inspired by matecloud's
 * DiscoveryMetadataRouteLocator.
 *
 * <p>Per-service errors are isolated (skipped) so one bad instance can never
 * break the route refresh or gateway startup.
 */
@Component
@RequiredArgsConstructor
public class MetadataRouteDefinitionLocator implements RouteDefinitionLocator {

    /** Discovery metadata key a service sets to opt into auto-routing (comma-separated path patterns). */
    public static final String PATH_META = "gateway.path";

    private final ReactiveDiscoveryClient discoveryClient;

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return discoveryClient.getServices()
                .flatMap(serviceId -> discoveryClient.getInstances(serviceId)
                        .next()
                        .map(instance -> instance.getMetadata() == null
                                ? ""
                                : instance.getMetadata().getOrDefault(PATH_META, ""))
                        .filter(path -> path != null && !path.isBlank())
                        .map(path -> buildRoute(serviceId, path.trim()))
                        .onErrorResume(ex -> Mono.empty()))
                .onErrorResume(ex -> Flux.empty());
    }

    private RouteDefinition buildRoute(String serviceId, String paths) {
        RouteDefinition route = new RouteDefinition();
        route.setId("dyn-" + serviceId);
        route.setUri(URI.create("lb://" + serviceId));
        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        int index = 0;
        for (String pattern : paths.split(",")) {
            String p = pattern.trim();
            if (!p.isEmpty()) {
                pathPredicate.addArg("_genkey_" + index++, p);
            }
        }
        route.setPredicates(List.of(pathPredicate));
        return route;
    }
}
