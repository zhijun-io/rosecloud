package io.rosecloud.starter.security.auth.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.core.model.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class RestAwareAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void writesApiResponseForForbiddenRequests() throws Exception {
        RestAwareAccessDeniedHandler handler = new RestAwareAccessDeniedHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tenants");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        ApiResponse<?> body = objectMapper.readValue(response.getContentAsString(), ApiResponse.class);
        assertThat(body.success()).isFalse();
        assertThat(body.code()).isEqualTo("security.forbidden");
        assertThat(body.message()).isEqualTo("无权限访问");
    }
}
