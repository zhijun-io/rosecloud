package io.rosecloud.starter.trace;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.actuate.autoconfigure.tracing.BraveAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class RoseCloudTraceAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BraveAutoConfiguration.class, MicrometerTracingAutoConfiguration.class));

    @Test
    void tracerBeanIsAutoConfiguredAndPopulatesMdc() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(Tracer.class);
            Tracer tracer = context.getBean(Tracer.class);
            try (Tracer.SpanInScope ignored = tracer.withSpan(tracer.nextSpan().name("test").start())) {
                assertThat(MDC.get("traceId")).isNotNull();
                assertThat(MDC.get("spanId")).isNotNull();
            }
            assertThat(MDC.get("traceId")).isNull();
        });
    }
}
