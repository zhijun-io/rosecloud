 package io.rosecloud.starter.security.context;
 
 import com.fasterxml.jackson.databind.ObjectMapper;
 import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.starter.security.auth.extractor.BearerTokenExtractor;
import io.rosecloud.common.security.session.SessionStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
 import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
 import org.springframework.security.web.util.matcher.RequestMatcher;
 import org.springframework.web.filter.OncePerRequestFilter;
 
 import java.io.IOException;
 
public class LogoutProcessingFilter extends OncePerRequestFilter {

    private final RequestMatcher matcher;
    private final BearerTokenExtractor tokenExtractor;
    private final SessionStore sessionStore;
    private final ObjectMapper objectMapper;

    public LogoutProcessingFilter(BearerTokenExtractor tokenExtractor,
                                  SessionStore sessionStore,
                                  ObjectMapper objectMapper) {
        this.matcher = PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/auth/logout");
        this.tokenExtractor = tokenExtractor;
        this.sessionStore = sessionStore;
        this.objectMapper = objectMapper;
    }
 
     @Override
     protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
         if (!matcher.matches(request)) {
             filterChain.doFilter(request, response);
             return;
         }
 
        try {
            String token = tokenExtractor.extract(request);
            sessionStore.revoke(token);
        } catch (Exception ignored) {
            // Even if token parsing fails, clear context
        }
 
         SecurityContextHolder.clearContext();
         response.setStatus(HttpServletResponse.SC_OK);
         response.setContentType(MediaType.APPLICATION_JSON_VALUE);
         objectMapper.writeValue(response.getWriter(), ApiResponse.ok());
     }
 }
