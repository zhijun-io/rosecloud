package io.rosecloud.system.service.impl;

import io.rosecloud.api.credential.CredentialApi;
import io.rosecloud.api.credential.CredentialSetRequest;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.domain.User;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.UserCredentialDao;
import io.rosecloud.system.persistence.UserCredentialEntity;
import io.rosecloud.system.persistence.UserDao;
import io.rosecloud.system.persistence.TenantDao;
import io.rosecloud.system.service.UserActivationService;
import io.rosecloud.system.config.UserActivationProperties;
import io.rosecloud.system.service.dto.UserActivationInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UserActivationServiceImpl implements UserActivationService {

    private static final long DEFAULT_VERSION = 1L;

    private final UserDao userDao;
    private final UserCredentialDao userCredentialDao;
    private final TenantDao tenantDao;
    private final CredentialApi credentialApi;
    private final UserActivationProperties properties;
    private final Map<String, Instant> lastResendAt = new ConcurrentHashMap<>();

    private static final int GLOBAL_MAX_RESENDS = 30;
    private static final long GLOBAL_WINDOW_SECONDS = 60;
    private final Object globalLock = new Object();
    private Instant globalWindowStart = Instant.EPOCH;
    private int globalResendCount = 0;

    @Override
    public UserActivationInfo check(String activateToken) {
        return findActivationByToken(activateToken)
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_ACTIVATION_TOKEN_INVALID));
    }

    @Override
    @Transactional
    public UserActivationInfo confirm(String activateToken, String password) {
        UserActivationInfo info = check(activateToken);
        if (info.used()) {
            throw new BizException(SystemErrorCode.USER_ACTIVATION_TOKEN_USED);
        }
        if (info.expired()) {
            throw new BizException(SystemErrorCode.USER_ACTIVATION_TOKEN_EXPIRED);
        }
        credentialApi.setPassword(info.userId(), new CredentialSetRequest(password));
        confirmActivation(info.userId());
        return findActivationByUsername(info.username())
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_ACTIVATION_TOKEN_INVALID));
    }

    @Override
    @Transactional
    public void resend(String username) {
        if (!acquireGlobalResendQuota()) {
            return;
        }
        UserActivationInfo info = findActivationByUsername(username).orElse(null);
        if (info == null || info.used()) {
            return;
        }
        Instant now = Instant.now();
        Instant last = lastResendAt.get(username);
        if (last != null && Duration.between(last, now).toSeconds() < properties.getResendCooldownSeconds()) {
            return;
        }
        lastResendAt.put(username, now);
        LocalDateTime sendTime = LocalDateTime.now();
        LocalDateTime expireTime = sendTime.plusHours(properties.getActivationTtlHours());
        String token = generateToken();
        long nextVersion = info.version() == null ? DEFAULT_VERSION : info.version() + 1;
        saveActivationToken(info.userId(), token, expireTime, sendTime, nextVersion);
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private boolean acquireGlobalResendQuota() {
        Instant now = Instant.now();
        synchronized (globalLock) {
            if (Duration.between(globalWindowStart, now).toSeconds() >= GLOBAL_WINDOW_SECONDS) {
                globalWindowStart = now;
                globalResendCount = 0;
            }
            if (globalResendCount >= GLOBAL_MAX_RESENDS) {
                return false;
            }
            globalResendCount++;
            return true;
        }
    }

    private Optional<UserActivationInfo> findActivationByToken(String activateToken) {
        if (activateToken == null || activateToken.isBlank()) {
            return Optional.empty();
        }
        Optional<UserCredentialEntity> credentialOpt = userCredentialDao.findByActivateToken(activateToken);
        if (credentialOpt.isEmpty()) {
            return Optional.empty();
        }
        UserCredentialEntity credential = credentialOpt.get();
        Optional<User> userOpt = userDao.findById(credential.getUserId());
        return userOpt.map(user -> toActivationInfo(user, credential));
    }

    private Optional<UserActivationInfo> findActivationByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        Optional<User> userOpt = userDao.findByEmailOrPhone(username);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        Optional<UserCredentialEntity> credentialOpt = userCredentialDao.findByUserId(user.getId());
        return credentialOpt.map(credential -> toActivationInfo(user, credential));
    }

    private void confirmActivation(Long userId) {
        UserCredentialEntity credential = userCredentialDao.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Missing user credential for userId=" + userId));
        credential.setActivateToken(null);
        credential.setExpireTime(null);
        credential.setUsedTime(LocalDateTime.now());
        userCredentialDao.update(credential);

        Optional<User> userOpt = userDao.findById(userId);
        userOpt.ifPresent(user -> {
            User updated = new User(user.getId(), user.getUsername(), user.getNickname(), 1, user.getTenantId(),
                    user.getAdditionalInfo(), user.getCreateTime(), user.getCreateBy(),
                    LocalDateTime.now(), userId);
            userDao.save(updated);
            activateFirstAdminTenant(user);
        });
    }

    private void activateFirstAdminTenant(User user) {
        String loginName = user.getUsername();
        if (loginName == null) {
            return;
        }
        Tenant tenant = tenantDao.findById(user.getTenantId()).orElse(null);
        if (tenant == null) {
            return;
        }
        if (!tenantDao.isAdminUser(user.getTenantId(), loginName)) {
            return;
        }
        if (tenant.getStatus() == null || tenant.getStatus() != TenantStatus.PENDING) {
            return;
        }
        tenantDao.updateStatus(user.getTenantId(), TenantStatus.ENABLED.code());
    }

    private void saveActivationToken(Long userId, String activateToken, LocalDateTime expireTime,
                                      LocalDateTime sendTime, Long version) {
        UserCredentialEntity credential = userCredentialDao.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Missing user credential for userId=" + userId));
        credential.setActivateToken(activateToken);
        credential.setExpireTime(expireTime);
        credential.setUsedTime(null);
        credential.setSendTime(sendTime);
        credential.setVersion(version);
        userCredentialDao.update(credential);
    }

    private UserActivationInfo toActivationInfo(User user, UserCredentialEntity credential) {
        return new UserActivationInfo(user.getId(), user.getUsername(), user.getTenantId(),
                credential.getActivateToken(),
                credential.getExpireTime(),
                credential.getUsedTime(),
                credential.getSendTime(),
                credential.getVersion());
    }
}
