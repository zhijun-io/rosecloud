 package io.rosecloud.starter.security.config;
 
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.starter.security.auth.extractor.BearerTokenExtractor;
import io.rosecloud.starter.security.auth.jwt.*;
import io.rosecloud.starter.security.auth.rest.*;
import io.rosecloud.starter.security.context.AuthExceptionHandler;
import io.rosecloud.starter.security.context.LogoutProcessingFilter;
import io.rosecloud.starter.security.session.InMemorySessionStore;
import io.rosecloud.common.security.session.SessionStore;
import io.rosecloud.starter.security.token.JwtTokenFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
 import org.springframework.security.authentication.AuthenticationManager;
 import org.springframework.security.authentication.ProviderManager;
 import org.springframework.security.config.annotation.web.builders.HttpSecurity;
 import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
 import org.springframework.security.config.http.SessionCreationPolicy;
 import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
 import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
 import org.springframework.web.cors.CorsConfiguration;
 import org.springframework.web.cors.CorsConfigurationSource;
 import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
 
 import java.util.List;
 import java.util.function.Consumer;
 import java.util.function.Function;
 
 @Configuration
 @EnableWebSecurity
 public class SecurityConfiguration {
 
     public static final String LOGIN_ENTRY_POINT = "/api/auth/login";
     public static final String REFRESH_ENTRY_POINT = "/api/auth/refresh";
     public static final String LOGOUT_ENTRY_POINT = "/api/auth/logout";
     public static final String TOKEN_BASED_AUTH_ENTRY_POINT = "/api/**";
 
    private final SecurityProperties properties;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    public SecurityConfiguration(SecurityProperties properties,
                                 @Lazy UserDetailsService userDetailsService,
                                 ObjectMapper objectMapper) {
        this.properties = properties;
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
    }
 
    @Bean
    @ConditionalOnMissingBean
    public SessionStore sessionStore() {
        return new InMemorySessionStore();
    }

    @Bean
    public JwtTokenFactory jwtTokenFactory() {
         return new JwtTokenFactory(properties);
     }
 
     @Bean
     public BearerTokenExtractor bearerTokenExtractor() {
         return new BearerTokenExtractor();
     }
 
    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpSecurityHeadersCustomizer httpSecurityHeadersCustomizer() {
         return HttpSecurityHeadersCustomizer.noop();
     }
 
     @Bean
    public RestAwareAuthenticationSuccessHandler restAwareAuthenticationSuccessHandler(
            JwtTokenFactory jwtTokenFactory, SessionStore sessionStore, ApplicationEventPublisher eventPublisher) {
        return new RestAwareAuthenticationSuccessHandler(jwtTokenFactory, sessionStore, eventPublisher, objectMapper);
    }
 
     @Bean
     public RestAwareAuthenticationFailureHandler restAwareAuthenticationFailureHandler(ApplicationEventPublisher eventPublisher) {
         return new RestAwareAuthenticationFailureHandler(eventPublisher, objectMapper);
     }
 
    @Bean
    public RestAuthenticationProvider restAuthenticationProvider(PasswordEncoder passwordEncoder) {
        return new RestAuthenticationProvider(userDetailsService, passwordEncoder);
    }
 
     @Bean
    public JwtAuthenticationProvider jwtAuthenticationProvider(
            JwtTokenFactory jwtTokenFactory, SessionStore sessionStore) {
        return new JwtAuthenticationProvider(jwtTokenFactory, sessionStore, userDetailsService);
    }
 
     @Bean
    public RefreshTokenAuthenticationProvider refreshTokenAuthenticationProvider(
            JwtTokenFactory jwtTokenFactory, SessionStore sessionStore) {
        return new RefreshTokenAuthenticationProvider(jwtTokenFactory, userDetailsService);
    }
 
     @Bean
     public AuthenticationManager authenticationManager(
             RestAuthenticationProvider restAuthenticationProvider,
             JwtAuthenticationProvider jwtAuthenticationProvider,
             RefreshTokenAuthenticationProvider refreshTokenAuthenticationProvider) {
         return new ProviderManager(
                 restAuthenticationProvider,
                 jwtAuthenticationProvider,
                 refreshTokenAuthenticationProvider);
     }
 
     @Bean
     public RestLoginProcessingFilter restLoginProcessingFilter(
             RestAwareAuthenticationSuccessHandler restAwareAuthenticationSuccessHandler,
             RestAwareAuthenticationFailureHandler restAwareAuthenticationFailureHandler,
             AuthenticationManager authenticationManager) {
         RestLoginProcessingFilter filter = new RestLoginProcessingFilter(
                 LOGIN_ENTRY_POINT,
                 restAwareAuthenticationSuccessHandler,
                 restAwareAuthenticationFailureHandler,
                 objectMapper);
         filter.setAuthenticationManager(authenticationManager);
         return filter;
     }
 
     @Bean
     public RefreshTokenProcessingFilter refreshTokenProcessingFilter(
             RestAwareAuthenticationSuccessHandler restAwareAuthenticationSuccessHandler,
             RestAwareAuthenticationFailureHandler restAwareAuthenticationFailureHandler,
             AuthenticationManager authenticationManager) {
         RefreshTokenProcessingFilter filter = new RefreshTokenProcessingFilter(
                 REFRESH_ENTRY_POINT,
                 restAwareAuthenticationSuccessHandler,
                 restAwareAuthenticationFailureHandler,
                 objectMapper);
         filter.setAuthenticationManager(authenticationManager);
         return filter;
     }
 
     @Bean
    public JwtTokenAuthenticationProcessingFilter jwtTokenAuthenticationProcessingFilter(
            RestAwareAuthenticationFailureHandler restAwareAuthenticationFailureHandler,
            BearerTokenExtractor bearerTokenExtractor,
            AuthenticationManager authenticationManager) {
        List<String> pathsToSkip = List.of(
                LOGIN_ENTRY_POINT,
                REFRESH_ENTRY_POINT,
                LOGOUT_ENTRY_POINT,
                "/api/noauth/**",
                "/api/public/**");
        SkipPathRequestMatcher matcher = new SkipPathRequestMatcher(pathsToSkip, TOKEN_BASED_AUTH_ENTRY_POINT);
        JwtTokenAuthenticationProcessingFilter filter = new JwtTokenAuthenticationProcessingFilter(
                matcher, restAwareAuthenticationFailureHandler, bearerTokenExtractor);
         filter.setAuthenticationManager(authenticationManager);
         return filter;
     }
 
     @Bean
    public LogoutProcessingFilter logoutProcessingFilter(
            BearerTokenExtractor bearerTokenExtractor,
            SessionStore sessionStore) {
        return new LogoutProcessingFilter(bearerTokenExtractor, sessionStore, objectMapper);
    }
 
    @Bean
    public AuthExceptionHandler authExceptionHandler() {
        return new AuthExceptionHandler(objectMapper);
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            AuthExceptionHandler authExceptionHandler,
            RestLoginProcessingFilter restLoginProcessingFilter,
            RefreshTokenProcessingFilter refreshTokenProcessingFilter,
            JwtTokenAuthenticationProcessingFilter jwtTokenAuthenticationProcessingFilter,
             LogoutProcessingFilter logoutProcessingFilter,
             HttpSecurityHeadersCustomizer headersCustomizer) throws Exception {
 
         http
             .cors(cors -> cors.configurationSource(corsConfigurationSource()))
             .csrf(csrf -> csrf.disable())
             .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
             .headers(headers -> {
                 headers.cacheControl(cache -> cache.disable());
                 headers.frameOptions(frame -> frame.sameOrigin());
                 headersCustomizer.customize(headers);
             })
             .authorizeHttpRequests(auth -> {
                 for (String path : properties.getPublicPaths()) {
                     auth.requestMatchers(path).permitAll();
                 }
                 auth.anyRequest().authenticated();
             })
            .addFilterBefore(authExceptionHandler, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(restLoginProcessingFilter, UsernamePasswordAuthenticationFilter.class)
             .addFilterBefore(refreshTokenProcessingFilter, UsernamePasswordAuthenticationFilter.class)
             .addFilterBefore(jwtTokenAuthenticationProcessingFilter, UsernamePasswordAuthenticationFilter.class)
             .addFilterBefore(logoutProcessingFilter, UsernamePasswordAuthenticationFilter.class);
 
         return http.build();
     }
 
     private CorsConfigurationSource corsConfigurationSource() {
         CorsConfiguration config = new CorsConfiguration();
         config.setAllowedOriginPatterns(List.of("*"));
         config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
         config.setAllowedHeaders(List.of("*"));
         config.setAllowCredentials(true);
         config.setMaxAge(3600L);
         UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
         source.registerCorsConfiguration("/**", config);
        return source;
    }
}
