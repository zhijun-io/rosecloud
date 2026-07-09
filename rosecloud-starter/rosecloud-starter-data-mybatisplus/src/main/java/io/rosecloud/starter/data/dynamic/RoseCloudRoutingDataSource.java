package io.rosecloud.starter.data.dynamic;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link AbstractRoutingDataSource} that delegates to {@link DataSourceRoute} to
 * pick the target datasource per call. The primary datasource is the fallback
 * for a {@code null} or unknown key. Routing is resolved at first connection
 * access within a transaction, so the route key should be stable for the
 * request/context scope.
 */
public class RoseCloudRoutingDataSource extends AbstractRoutingDataSource {

    private final DataSourceRoute route;
    private final String primaryKey;

    public RoseCloudRoutingDataSource(Map<String, DataSource> targetDataSources, String primaryKey,
                                      DataSourceRoute route) {
        this.route = route;
        this.primaryKey = primaryKey;
        Map<Object, Object> targets = new LinkedHashMap<>();
        targetDataSources.forEach(targets::put);
        setTargetDataSources(targets);
        setDefaultTargetDataSource(targetDataSources.get(primaryKey));
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String key = route.resolveKey();
        return key == null ? primaryKey : key;
    }
}
