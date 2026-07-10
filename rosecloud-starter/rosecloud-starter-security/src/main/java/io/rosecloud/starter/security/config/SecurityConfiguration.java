package io.rosecloud.starter.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.common.security.session.SessionStore;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.starter.security.token.BearerTokenExtractor;
import io.rosecloud.starter.security.web.InternalApiAuthenticationFilter;
import io.rosecloud.starter.security.auth.jwt.*;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import io.rosecloud.starter.security.auth.rest.RestAuthenticationProvider;
import io.rosecloud.starter.security.auth.rest.RestAwareAccessDeniedHandler;
import io.rosecloud.starter.security.auth.rest.RestAwareAuthenticationEntryPoint;
import io.rosecloud.starter.security.auth.rest.RestAwareAuthenticationFailureHandler;
import io.rosecloud.starter.security.auth.rest.RestAwareAuthenticationSuccessHandler;
import io.rosecloud.starter.security.auth.rest.RestLoginProcessingFilter;
import io.rosecloud.starter.security.context.LogoutProcessingFilter;
import io.rosecloud.starter.security.session.InMemorySessionStore;
import io.rosecloud.starter.security.session.RedisSessionStore;
import io.rosecloud.starter.security.token.JwtTokenFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.LinkedHashSet;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    public static final String LOGIN_ENTRY_POINT = ServiceMetadata.API_PREFIX + "/auth/login";
    public static final String REFRESH_ENTRY_POINT = ServiceMetadata.API_PREFIX + "/auth/refresh";
    public static final String LOGOUT_ENTRY_POINT = ServiceMetadata.API_PREFIX + "/auth/logout";
    public static final String TOKEN_BASED_AUTH_ENTRY_POINT = ServiceMetadata.API_PREFIX + "/**";

    private final SecurityProperties properties;
    private final ObjectMapper objectMapper;

    public SecurityConfiguration(SecurityProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            RestLoginProcessingFilter restLoginProcessingFilter,
            RefreshTokenProcessingFilter refreshTokenProcessingFilter,
            JwtTokenAuthenticationProcessingFilter jwtTokenAuthenticationProcessingFilter,
            LogoutProcessingFilter logoutProcessingFilter) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAwareAuthenticationEntryPoint())
                        .accessDeniedHandler(restAwareAccessDeniedHandler()))
                .headers(headers -> {
                    headers.cacheControl(cache -> cache.disable());
                    headers.frameOptions(frame -> frame.sameOrigin());
                    headers.httpStrictTransportSecurity(hsts ->
                            hsts.includeSubDomains(true).maxAgeInSeconds(31536000));
                    headers.contentTypeOptions(Customizer.withDefaults());
                    headers.referrerPolicy(Customizer.withDefaults());
                })
                .authorizeHttpRequests(auth -> {
                    for (String path : properties.getPublicPaths()) {
                        auth.requestMatchers(path).permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(restLoginProcessingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(refreshTokenProcessingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtTokenAuthenticationProcessingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(logoutProcessingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new InternalApiAuthenticationFilter(properties), JwtTokenAuthenticationProcessingFilter.class);

        return http.build();
    }

    /**
     * Session store used for JWT revocation lookups. When a {@link StringRedisTemplate}
     * is available (Redis on the classpath and configured) a shared
     * {@link RedisSessionStore} is used so that logout/revocation propagates across all
     * services; otherwise it falls back to an in-process {@link InMemorySessionStore}.
     *
     * <p>Using an in-memory store in a multi-service deployment would make every service
     * treat externally-issued tokens as revoked — so Redis is strongly preferred whenever
     * present.
     */
    @Configuration(proxyBeanMethods = false)
    static class SessionStoreConfiguration {

        @Bean
        @ConditionalOnMissingBean(SessionStore.class)
        @ConditionalOnClass(StringRedisTemplate.class)
        public SessionStore redisAwareSessionStore(ObjectProvider<StringRedisTemplate> redisTemplate) {
            StringRedisTemplate template = redisTemplate.getIfAvailable();
            return template != null ? new RedisSessionStore(template) : new InMemorySessionStore();
        }

        @Bean
        @ConditionalOnMissingBean(SessionStore.class)
        public SessionStore inMemorySessionStore() {
            return new InMemorySessionStore();
        }
    }

    @Bean
    public JwtTokenFactory jwtTokenFactory() {
        return new JwtTokenFactory(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestAwareAuthenticationSuccessHandler restAwareAuthenticationSuccessHandler(
            JwtTokenFactory jwtTokenFactory, SessionStore sessionStore, ApplicationEventPublisher eventPublisher) {
        return new RestAwareAuthenticationSuccessHandler(jwtTokenFactory, sessionStore, eventPublisher,
                objectMapper, properties.getRefreshTokenExpirationSeconds());
    }

    @Bean
    public RestAwareAuthenticationFailureHandler restAwareAuthenticationFailureHandler(ApplicationEventPublisher eventPublisher) {
        return new RestAwareAuthenticationFailureHandler(eventPublisher, objectMapper);
    }

    @Bean
    public RestAwareAuthenticationEntryPoint restAwareAuthenticationEntryPoint() {
        return new RestAwareAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    public RestAwareAccessDeniedHandler restAwareAccessDeniedHandler() {
        return new RestAwareAccessDeniedHandler(objectMapper);
    }

    @Bean
    public RestAuthenticationProvider restAuthenticationProvider(
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        return new RestAuthenticationProvider(userDetailsService, passwordEncoder);
    }

    @Bean
    public JwtAuthenticationProvider jwtAuthenticationProvider(
            JwtTokenFactory jwtTokenFactory,
            SessionStore sessionStore,
            UserDetailsService userDetailsService) {
        return new JwtAuthenticationProvider(jwtTokenFactory, sessionStore, userDetailsService);
    }

    @Bean
    public RefreshTokenAuthenticationProvider refreshTokenAuthenticationProvider(
            JwtTokenFactory jwtTokenFactory,
            SessionStore sessionStore,
            UserDetailsService userDetailsService) {
        return new RefreshTokenAuthenticationProvider(jwtTokenFactory, sessionStore, userDetailsService);
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
            AuthenticationManager authenticationManager) {
        List<String> pathsToSkip = new java.util.ArrayList<>(new LinkedHashSet<>(List.of(
                properties.getPublicPaths()
        )));
        pathsToSkip.add(LOGIN_ENTRY_POINT);
        pathsToSkip.add(REFRESH_ENTRY_POINT);
        pathsToSkip.add(LOGOUT_ENTRY_POINT);
        List<RequestMatcher> internalSkips = List.of(
                new RequestHeaderRequestMatcher(InternalApiAuthenticationFilter.INTERNAL_HEADER));
        SkipPathRequestMatcher matcher = new SkipPathRequestMatcher(pathsToSkip, internalSkips, TOKEN_BASED_AUTH_ENTRY_POINT);
        JwtTokenAuthenticationProcessingFilter filter = new JwtTokenAuthenticationProcessingFilter(
                matcher, restAwareAuthenticationFailureHandler, new BearerTokenExtractor());
        filter.setAuthenticationManager(authenticationManager);
        return filter;
    }

    @Bean
    public LogoutProcessingFilter logoutProcessingFilter(
            SessionStore sessionStore) {
        return new LogoutProcessingFilter(new BearerTokenExtractor(), sessionStore, objectMapper);
    }

    private CorsConfigurationSource corsConfigurationSource() {
        SecurityProperties.Cors cors = properties.getCors();
        if (cors.isAllowCredentials()
                && cors.getAllowedOrigins().stream().anyMatch(o -> "*".equals(o))) {
            throw new IllegalStateException(
                    "CORS allowCredentials=true must not be combined with a wildcard '*' origin");
        }
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(cors.getAllowedOrigins());
        config.setAllowedMethods(cors.getAllowedMethods());
        config.setAllowedHeaders(cors.getAllowedHeaders());
        config.setAllowCredentials(cors.isAllowCredentials());
        config.setMaxAge(cors.getMaxAge());
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
