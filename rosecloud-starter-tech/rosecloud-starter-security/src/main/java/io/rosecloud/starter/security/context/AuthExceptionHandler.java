 package io.rosecloud.starter.security.context;
 
 import com.fasterxml.jackson.databind.ObjectMapper;
 import io.rosecloud.common.core.model.ApiResponse;
 import jakarta.servlet.FilterChain;
 import jakarta.servlet.ServletException;
 import jakarta.servlet.http.HttpServletRequest;
 import jakarta.servlet.http.HttpServletResponse;
 import org.springframework.http.MediaType;
 import org.springframework.security.core.AuthenticationException;
 import org.springframework.security.core.context.SecurityContextHolder;
 import org.springframework.web.filter.OncePerRequestFilter;
 
 import java.io.IOException;
 
 public class AuthExceptionHandler extends OncePerRequestFilter {
 
     private final ObjectMapper objectMapper;
 
     public AuthExceptionHandler(ObjectMapper objectMapper) {
         this.objectMapper = objectMapper;
     }
 
     @Override
     protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
         try {
             filterChain.doFilter(request, response);
         } catch (AuthenticationException e) {
             SecurityContextHolder.clearContext();
             response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
             response.setContentType(MediaType.APPLICATION_JSON_VALUE);
             objectMapper.writeValue(response.getWriter(),
                     ApiResponse.failure("UNAUTHORIZED", e.getMessage()));
         }
     }
 }
