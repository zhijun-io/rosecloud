package io.rosecloud.starter.security.config;
import lombok.RequiredArgsConstructor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.api.user.TenantLookupApi;
import io.rosecloud.starter.security.web.TenantWriteGuardFilter;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.common.security.session.SessionStore;
import io.rosecloud.starter.security.auth.LoginTenantResolver;
import io.rosecloud.starter.security.auth.BruteForceProtection;
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
import io.rosecloud.api.session.SessionFeignApi;
import io.rosecloud.starter.security.session.FeignSessionStore;
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

@RequiredArgsConstructor
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
    private final ObjectProvider<TenantLookupApi> tenantLookupApiProvider;
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
                .addFilterBefore(new InternalApiAuthenticationFilter(properties), JwtTokenAuthenticationProcessingFilter.class)
                .addFilterAfter(new TenantWriteGuardFilter(tenantLookupApiProvider,
                        properties.getTenantWriteGuard().isFailClosed()), JwtTokenAuthenticationProcessingFilter.class);

        return http.build();
    }

    /**
     * Session store used for JWT revocation lookups.
     *
     * <p>Precedence: when a {@link SessionApi} Feign client is available the store is backed by
     * auth's {@link FeignSessionStore}, so revocation is enforced centrally in auth (the single
     * authoritative session owner per the IAM boundary) and propagates to every service without a
     * shared Redis. When Feign is not enabled for the session API it falls back to a shared
     * {@link RedisSessionStore} (if Redis is on the classpath) or an in-process
     * {@link InMemorySessionStore} (single-instance / tests only).
     */
    @Configuration(proxyBeanMethods = false)
    static class SessionStoreConfiguration {

        private static final org.slf4j.Logger log =
                org.slf4j.LoggerFactory.getLogger(SessionStoreConfiguration.class);

        @Bean
        @ConditionalOnMissingBean(SessionStore.class)
        public SessionStore sessionStore(ObjectProvider<SessionFeignApi> sessionApi,
                                        ObjectProvider<StringRedisTemplate> redisTemplate) {
            SessionFeignApi api = sessionApi.getIfAvailable();
            if (api != null) {
                log.info("Using FeignSessionStore (sessions owned by auth). Revocation is enforced "
                        + "centrally in auth; isRevoked adds one Feign round-trip per request.");
                return new FeignSessionStore(api);
            }
            StringRedisTemplate template = redisTemplate.getIfAvailable();
            if (template != null) {
                return new RedisSessionStore(template);
            }
            log.warn("No SessionApi Feign client and no Redis — using InMemorySessionStore. Revocation is "
                    + "local-only and lost on restart; do not use in multi-service deployments.");
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
            JwtTokenFactory jwtTokenFactory, SessionStore sessionStore, ApplicationEventPublisher eventPublisher,
            ObjectProvider<LoginTenantResolver> loginTenantResolver) {
        return new RestAwareAuthenticationSuccessHandler(jwtTokenFactory, sessionStore, eventPublisher,
                objectMapper, loginTenantResolver.getIfAvailable(), properties.getRefreshTokenExpirationSeconds(),
                properties.getTokenBinding().isEnabled());
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
    public BruteForceProtection bruteForceProtection(ObjectProvider<StringRedisTemplate> redisTemplate) {
        return new BruteForceProtection(properties.getBruteForce(), redisTemplate.getIfAvailable());
    }

    @Bean
    public RestAuthenticationProvider restAuthenticationProvider(
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder,
            BruteForceProtection bruteForceProtection) {
        return new RestAuthenticationProvider(userDetailsService, passwordEncoder, bruteForceProtection);
    }

    @Bean
    public JwtAuthenticationProvider jwtAuthenticationProvider(
            JwtTokenFactory jwtTokenFactory,
            SessionStore sessionStore,
            ObjectProvider<TenantLookupApi> tenantLookupApiProvider) {
        return new JwtAuthenticationProvider(jwtTokenFactory, sessionStore,
                tenantLookupApiProvider.getIfAvailable());
    }

    @Bean
    public RefreshTokenAuthenticationProvider refreshTokenAuthenticationProvider(
            JwtTokenFactory jwtTokenFactory,
            SessionStore sessionStore,
            UserDetailsService userDetailsService,
            ObjectProvider<TenantLookupApi> tenantLookupApiProvider,
            BruteForceProtection bruteForceProtection) {
        return new RefreshTokenAuthenticationProvider(jwtTokenFactory, sessionStore, userDetailsService,
                tenantLookupApiProvider.getIfAvailable(), bruteForceProtection);
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
                restAwareAuthenticationFailureHandler);
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
                restAwareAuthenticationFailureHandler);
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
