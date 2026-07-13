package io.rosecloud.starter.security.auth.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.core.model.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class RestAwareAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void writesApiResponseForUnauthenticatedRequests() throws Exception {
        RestAwareAuthenticationEntryPoint entryPoint = new RestAwareAuthenticationEntryPoint(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response,
                new AuthenticationCredentialsNotFoundException("Full authentication is required"));

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        ApiResponse<?> body = objectMapper.readValue(response.getContentAsString(), ApiResponse.class);
        assertThat(body.success()).isFalse();
        assertThat(body.code()).isEqualTo("security.unauthorized");
        assertThat(body.message()).isEqualTo("未授权，需要登录");
    }
}
