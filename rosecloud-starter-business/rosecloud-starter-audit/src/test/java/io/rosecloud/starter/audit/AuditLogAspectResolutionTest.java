package io.rosecloud.starter.audit;

import io.rosecloud.starter.audit.support.ClientIpResolver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogAspectResolutionTest {

    @Test
    void severity_falls_back_when_blank() {
        assertThat(AuditLogAspect.resolveSeverity("", false)).isEqualTo("ERROR");
        assertThat(AuditLogAspect.resolveSeverity("", true)).isEqualTo("INFO");
        assertThat(AuditLogAspect.resolveSeverity("WARN", true)).isEqualTo("WARN");
        assertThat(AuditLogAspect.resolveSeverity("CRITICAL", false)).isEqualTo("CRITICAL");
    }

    @Test
    void spel_resolves_named_argument() {
        String id = AuditLogAspect.resolveExpression("#req.id", new String[]{"req"}, new Object[]{
                new Req("u-42")
        });
        assertThat(id).isEqualTo("u-42");
    }

    static class Req {
        private final String id;

        Req(String id) {
            this.id = id;
        }

        @SuppressWarnings("unused")
        public String getId() {
            return id;
        }
    }

    @Test
    void spel_returns_null_for_blank_or_invalid() {
        assertThat(AuditLogAspect.resolveExpression("", new String[]{"id"}, new Object[]{"x"})).isNull();
        assertThat(AuditLogAspect.resolveExpression("#missing", new String[]{"id"}, new Object[]{"x"})).isNull();
    }

    @Test
    void clientIpResolver_returns_null_without_request_context() {
        assertThat(ClientIpResolver.resolve()).isNull();
    }
}
