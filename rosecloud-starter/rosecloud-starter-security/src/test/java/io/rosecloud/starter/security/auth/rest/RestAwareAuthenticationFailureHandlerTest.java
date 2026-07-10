package io.rosecloud.starter.security.auth.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.core.model.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RestAwareAuthenticationFailureHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void writesGenericApiResponseOnAuthenticationException() throws Exception {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        RestAwareAuthenticationFailureHandler handler = new RestAwareAuthenticationFailureHandler(publisher, objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");

        assertResponse(handler, request, new BadCredentialsException("bad credentials"));
        assertResponse(handler, request, new UsernameNotFoundException("user not found"));
        assertResponse(handler, request, new DisabledException("disabled"));
    }

    private void assertResponse(RestAwareAuthenticationFailureHandler handler,
                                MockHttpServletRequest request,
                                Exception exception) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationFailure(request, response, (org.springframework.security.core.AuthenticationException) exception);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        ApiResponse<?> body = objectMapper.readValue(response.getContentAsString(), ApiResponse.class);
        assertThat(body.success()).isFalse();
        assertThat(body.code()).isEqualTo("security.bad_credentials");
        assertThat(body.message()).isEqualTo("用户名或密码错误");
        assertThat(body.data()).isNull();
    }
}
