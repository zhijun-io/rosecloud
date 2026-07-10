package io.rosecloud.auth.service;

import io.rosecloud.api.user.TenantAccessCandidate;
import io.rosecloud.api.user.UserTenantApi;
import io.rosecloud.auth.service.dto.TenantSelectionResponse;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import io.rosecloud.common.security.model.LoginSession;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.session.SessionStore;
import io.rosecloud.common.security.token.JwtPair;
import io.rosecloud.starter.security.auth.LoginTenantResolver;
import io.rosecloud.starter.security.config.SecurityProperties;
import io.rosecloud.starter.security.token.JwtTokenFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class TenantSelectionService implements LoginTenantResolver {

    private static final String SYSTEM_TENANT_ID = "ROOT";
    private static final String LAST_ACTIVE_TENANT_KEY_PREFIX = "auth:last-tenant:";
    private static final Duration LAST_ACTIVE_TENANT_TTL = Duration.ofDays(180);

    private final UserTenantApi userTenantApi;
    private final StringRedisTemplate redisTemplate;
    private final SessionStore sessionStore;
    private final JwtTokenFactory tokenFactory;
    private final SecurityProperties securityProperties;

    public TenantSelectionService(UserTenantApi userTenantApi,
                                  StringRedisTemplate redisTemplate,
                                  SessionStore sessionStore,
                                  JwtTokenFactory tokenFactory,
                                  SecurityProperties securityProperties) {
        this.userTenantApi = userTenantApi;
        this.redisTemplate = redisTemplate;
        this.sessionStore = sessionStore;
        this.tokenFactory = tokenFactory;
        this.securityProperties = securityProperties;
    }

    public TenantSelectionResponse getSelection(SecurityUser securityUser) {
        String rememberedTenantId = loadRememberedTenant(securityUser.getUserId()).orElse(null);
        List<TenantAccessCandidate> switchableTenants = loadSwitchableTenants(securityUser);
        return new TenantSelectionResponse(
                normalizeTenantId(securityUser.getTenantId()),
                rememberedTenantId,
                switchableTenants);
    }

    public JwtPair switchTenant(SecurityUser securityUser, String currentToken, String targetTenantId,
                                String clientIp, String userAgent) {
        String resolvedTenantId = normalizeTenantId(targetTenantId);
        List<TenantAccessCandidate> switchableTenants = loadSwitchableTenants(securityUser);
        if (switchableTenants.stream().noneMatch(candidate -> candidate.tenantId().equals(resolvedTenantId))) {
            throw new BizException(SecurityErrorCode.FORBIDDEN);
        }

        rememberTenant(securityUser.getUserId(), resolvedTenantId);
        JwtPair tokenPair = tokenFactory.createTokenPair(securityUser, resolvedTenantId);
        Instant now = Instant.now();
        sessionStore.save(new LoginSession(
                java.util.UUID.randomUUID().toString(),
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                securityUser.getUserId(),
                securityUser.getUsername(),
                securityUser.getNickname(),
                clientIp,
                truncate(userAgent, 512),
                now,
                now.plusSeconds(securityProperties.getRefreshTokenExpirationSeconds())));
        sessionStore.revoke(currentToken);
        return tokenPair;
    }

    @Override
    public String resolveInitialTenant(SecurityUser securityUser) {
        String rememberedTenantId = loadRememberedTenant(securityUser.getUserId()).orElse(null);
        List<TenantAccessCandidate> switchableTenants = loadSwitchableTenants(securityUser);

        String resolvedTenantId = selectInitialTenant(rememberedTenantId, securityUser.getTenantId(),
                switchableTenants);
        rememberTenant(securityUser.getUserId(), resolvedTenantId);
        return resolvedTenantId;
    }

    private String selectInitialTenant(String rememberedTenantId, String homeTenantId,
                                       List<TenantAccessCandidate> switchableTenants) {
        if (isSelectable(rememberedTenantId, switchableTenants)) {
            return rememberedTenantId;
        }
        String normalizedHomeTenantId = normalizeTenantId(homeTenantId);
        if (isSelectable(normalizedHomeTenantId, switchableTenants)) {
            return normalizedHomeTenantId;
        }
        if (!switchableTenants.isEmpty()) {
            return switchableTenants.get(0).tenantId();
        }
        if (SYSTEM_TENANT_ID.equals(normalizedHomeTenantId)) {
            return normalizedHomeTenantId;
        }
        // No usable tenant: the home tenant is not selectable (e.g. disabled or
        // expired) and the user has no other tenant to operate under. Refuse to
        // resolve a tenant so the login is rejected instead of silently issuing a
        // token under a stale tenant context.
        throw new BizException(SecurityErrorCode.FORBIDDEN);
    }

    private List<TenantAccessCandidate> loadSwitchableTenants(SecurityUser securityUser) {
        ApiResponse<List<TenantAccessCandidate>> response = userTenantApi.listTenantCandidates(securityUser.getUserId());
        if (response == null || !response.success() || response.data() == null) {
            return List.of();
        }
        return response.data().stream()
                .filter(TenantAccessCandidate::selectable)
                .toList();
    }

    private Optional<String> loadRememberedTenant(Long userId) {
        String value = redisTemplate.opsForValue().get(lastTenantKey(userId));
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(normalizeTenantId(value));
    }

    private void rememberTenant(Long userId, String tenantId) {
        redisTemplate.opsForValue().set(lastTenantKey(userId), normalizeTenantId(tenantId), LAST_ACTIVE_TENANT_TTL);
    }

    private static String lastTenantKey(Long userId) {
        return LAST_ACTIVE_TENANT_KEY_PREFIX + userId;
    }

    private static String normalizeTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return "";
        }
        return tenantId.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isSelectable(String tenantId, List<TenantAccessCandidate> switchableTenants) {
        if (tenantId == null || tenantId.isBlank()) {
            return false;
        }
        return switchableTenants.stream().anyMatch(candidate -> candidate.tenantId().equals(tenantId));
    }

    private static String truncate(String value, int max) {
        return value != null && value.length() > max ? value.substring(0, max) : value;
    }
}
