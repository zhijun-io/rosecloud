package io.rosecloud.starter.trace.web;

import io.rosecloud.starter.trace.TraceHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TraceContextFilterTest {

    @Test
    void reusesInboundTraceIdForPropagation() throws Exception {
        TraceContextFilter filter = new TraceContextFilter();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceHeaders.TRACE_ID, "client-trace");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seen = new AtomicReference<>();

        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                seen.set(((jakarta.servlet.http.HttpServletRequest) req).getHeader(TraceHeaders.TRACE_ID));
            }
        };

        filter.doFilter(request, response, chain);

        assertThat(seen.get()).isEqualTo("client-trace");
        assertThat(response.getHeader(TraceHeaders.TRACE_ID)).isEqualTo("client-trace");
    }

    @Test
    void generatesFreshTraceIdWhenAbsent() throws Exception {
        TraceContextFilter filter = new TraceContextFilter();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seen = new AtomicReference<>();

        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                seen.set(((jakarta.servlet.http.HttpServletRequest) req).getHeader(TraceHeaders.TRACE_ID));
            }
        };

        filter.doFilter(request, response, chain);

        assertThat(seen.get()).isNotBlank().hasSize(32);
        assertThat(response.getHeader(TraceHeaders.TRACE_ID)).isEqualTo(seen.get());
    }
}
