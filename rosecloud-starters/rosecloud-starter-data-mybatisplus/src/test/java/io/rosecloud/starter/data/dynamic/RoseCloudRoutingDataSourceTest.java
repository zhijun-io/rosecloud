package io.rosecloud.starter.data.dynamic;

import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the routing datasource selects the configured target by route key
 * and falls back to the primary for null/unknown keys. Uses in-memory H2 so no
 * external database is required.
 */
class RoseCloudRoutingDataSourceTest {

    @Test
    void routesByKeyAndFallsBackToPrimary() throws Exception {
        Map<String, DataSource> targets = new LinkedHashMap<>();
        targets.put("primary", markerDatasource("primary", "P"));
        targets.put("business", markerDatasource("business", "B"));

        AtomicReference<String> routeKey = new AtomicReference<>();
        RoseCloudRoutingDataSource ds = new RoseCloudRoutingDataSource(targets, "primary", routeKey::get);
        ds.afterPropertiesSet();

        routeKey.set(null);
        assertThat(markerOf(ds)).isEqualTo("P");

        routeKey.set("business");
        assertThat(markerOf(ds)).isEqualTo("B");

        routeKey.set("unknown");
        assertThat(markerOf(ds)).isEqualTo("P");
    }

    private static DataSource markerDatasource(String name, String marker) throws SQLException {
        DataSource ds = DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1")
                .username("sa")
                .password("")
                .build();
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE marker(val VARCHAR(10))");
            s.execute("INSERT INTO marker VALUES ('" + marker + "')");
        }
        return ds;
    }

    private static String markerOf(DataSource ds) throws SQLException {
        try (Connection c = ds.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT val FROM marker")) {
            rs.next();
            return rs.getString(1);
        }
    }
}
