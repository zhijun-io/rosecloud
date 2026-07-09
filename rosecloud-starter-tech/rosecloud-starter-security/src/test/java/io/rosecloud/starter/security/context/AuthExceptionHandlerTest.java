package io.rosecloud.starter.security.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.core.model.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class AuthExceptionHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void writesApiResponseForAuthenticationExceptions() throws Exception {
        AuthExceptionHandler handler = new AuthExceptionHandler(objectMapper);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.doFilter(new MockHttpServletRequest("GET", "/api/test"), response, throwingChain(
                new BadCredentialsException("nope")));

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        ApiResponse<?> body = objectMapper.readValue(response.getContentAsString(), ApiResponse.class);
        assertThat(body.success()).isFalse();
        assertThat(body.code()).isEqualTo("security.unauthorized");
        assertThat(body.message()).isEqualTo("nope");
    }

    @Test
    void writesApiResponseForAccessDeniedExceptions() throws Exception {
        AuthExceptionHandler handler = new AuthExceptionHandler(objectMapper);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.doFilter(new MockHttpServletRequest("GET", "/api/test"), response, throwingChain(
                new AccessDeniedException("denied")));

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        ApiResponse<?> body = objectMapper.readValue(response.getContentAsString(), ApiResponse.class);
        assertThat(body.success()).isFalse();
        assertThat(body.code()).isEqualTo("security.forbidden");
        assertThat(body.message()).isEqualTo("denied");
    }

    private static FilterChain throwingChain(RuntimeException ex) {
        return (request, response) -> {
            throw ex;
        };
    }
}
