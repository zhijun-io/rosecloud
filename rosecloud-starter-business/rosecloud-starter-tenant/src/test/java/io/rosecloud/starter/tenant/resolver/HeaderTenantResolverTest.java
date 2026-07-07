package io.rosecloud.starter.tenant.resolver;

import io.rosecloud.common.security.SecurityHeaders;
import io.rosecloud.starter.tenant.core.TenantProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HeaderTenantResolverTest {

    @Test
    void alwaysResolvesTenantIdFromSecurityHeader() {
        HeaderTenantResolver resolver = new HeaderTenantResolver();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Custom-Tenant", "200");
        request.addHeader(SecurityHeaders.TENANT_ID, "100");

        assertEquals(100L, resolver.resolve(request));
    }

    @Test
    void returnsNullWhenFixedTenantHeaderIsMissing() {
        HeaderTenantResolver resolver = new HeaderTenantResolver();

        HttpServletRequest request = new MockHttpServletRequest();

        assertNull(resolver.resolve(request));
    }
}
