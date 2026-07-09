package io.rosecloud.starter.security.auth.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.core.model.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RestAwareAuthenticationFailureHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void writesApiResponseOnAuthenticationException() throws Exception {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        RestAwareAuthenticationFailureHandler handler = new RestAwareAuthenticationFailureHandler(publisher, objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad credentials"));

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        ApiResponse<?> body = objectMapper.readValue(response.getContentAsString(), ApiResponse.class);
        assertThat(body.success()).isFalse();
        assertThat(body.code()).isEqualTo("security.bad_credentials");
        assertThat(body.message()).isEqualTo("用户名或密码错误");
        assertThat(body.data()).isNull();
    }
}
