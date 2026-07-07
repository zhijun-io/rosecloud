package io.rosecloud.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Gateway security config under {@code rosecloud.gateway.security.*}. The
 * white-list matches request paths (Ant-style) that bypass JWT verification,
 * e.g. login/refresh and actuator.
 */
@ConfigurationProperties(prefix = "rosecloud.gateway.security")
public class GatewaySecurityProperties {

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
