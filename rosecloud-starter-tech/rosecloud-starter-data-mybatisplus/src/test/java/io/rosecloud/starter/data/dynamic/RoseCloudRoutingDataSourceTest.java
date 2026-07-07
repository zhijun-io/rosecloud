package io.rosecloud.starter.data.dynamic;

import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
 * and falls back to the primary for null/unknown keys. Uses MySQL Testcontainers
 * so the test exercises the same database family as production.
 */
@Testcontainers
class RoseCloudRoutingDataSourceTest {

    @Container
    static final MySQLContainer<?> primary = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("primary")
            .withUsername("rosecloud")
            .withPassword("rosecloud123");

    @Container
    static final MySQLContainer<?> business = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("business")
            .withUsername("rosecloud")
            .withPassword("rosecloud123");

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
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url(name.equals("primary") ? primary.getJdbcUrl() : business.getJdbcUrl())
                .username(name.equals("primary") ? primary.getUsername() : business.getUsername())
                .password(name.equals("primary") ? primary.getPassword() : business.getPassword())
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
