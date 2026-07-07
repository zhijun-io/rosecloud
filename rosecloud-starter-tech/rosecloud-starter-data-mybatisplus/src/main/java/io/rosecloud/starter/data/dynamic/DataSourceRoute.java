package io.rosecloud.starter.data.dynamic;

/**
 * Routing strategy for the dynamic datasource: returns the key of the target
 * datasource to use for the current context (e.g. a tenant id mapped to a
 * datasource). Returning {@code null} selects the primary datasource. Provide
 * a bean to implement coarse-grained data routing; the default routes to
 * primary everywhere.
 */
@FunctionalInterface
public interface DataSourceRoute {

    String resolveKey();
}
