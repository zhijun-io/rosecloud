package io.rosecloud.monolith;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Monolith security config under {@code rosecloud.monolith.security.*}. The
 * white-list matches request paths (Ant-style) that bypass JWT verification,
 * e.g. login/refresh and actuator — mirroring the gateway's white-list.
 */
@ConfigurationProperties(prefix = "rosecloud.monolith.security")
public class MonolithSecurityProperties {

    private List<String> whiteList = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/actuator/**");

    public List<String> getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(List<String> whiteList) {
        this.whiteList = whiteList;
    }
}
