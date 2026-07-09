package io.rosecloud.starter.trace;

import io.rosecloud.starter.trace.web.TraceContextFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Server-side trace wiring. Servlet apps get a request filter that generates a
 * new trace id for each request; reactive gateway apps get a matching global
 * filter. Either implementation can be swapped out later by a third-party
 * tracing system.
 */
@AutoConfiguration
public class TraceAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = "jakarta.servlet.Filter")
    static class ServletTraceConfiguration {

        @Bean
        @SuppressWarnings({"rawtypes", "unchecked"})
        public FilterRegistrationBean traceContextFilterRegistration() {
            FilterRegistrationBean registration = new FilterRegistrationBean();
            registration.setFilter(new TraceContextFilter());
            registration.addUrlPatterns("/*");
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
            registration.setAsyncSupported(true);
            return registration;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    static class ReactiveTraceConfiguration {

        @Bean
        public io.rosecloud.starter.trace.gateway.TraceIdGlobalFilter traceIdGlobalFilter() {
            return new io.rosecloud.starter.trace.gateway.TraceIdGlobalFilter();
        }
    }
}
