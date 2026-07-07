package io.rosecloud.starter.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorJsonTest {

    @Test
    void escapesQuoteAndBackslashInMessage() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ErrorJson.write(response, 401, SecurityErrorCode.INVALID_TOKEN, "bad \"quote\" and \\ slash");

        assertThat(response.getStatus()).isEqualTo(401);
        String body = response.getContentAsString();
        // Raw quotes must not appear unescaped inside the JSON envelope.
        assertThat(body).contains("\\\"quote\\\"").contains("\\\\ slash");
        assertThat(body).doesNotContain("\"bad \"quote\"");
        // The produced envelope is well-formed JSON.
        assertThat(body).startsWith("{\"success\":false,\"code\":\"security.invalid_token\"");
    }

    @Test
    void escapesControlCharactersInCodeAndMessage() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ErrorJson.write(response, 401, SecurityErrorCode.INVALID_TOKEN, "line1\nline2\ttab");

        String body = response.getContentAsString();
        assertThat(body).contains("\\n").contains("\\t");
        assertThat(body).doesNotContain("\n").doesNotContain("\t");
    }

    @Test
    void escapeHandlesNullAndEmpty() {
        assertThat(ErrorJson.escape(null)).isEmpty();
        assertThat(ErrorJson.escape("")).isEmpty();
        assertThat(ErrorJson.escape("plain")).isEqualTo("plain");
    }
}
