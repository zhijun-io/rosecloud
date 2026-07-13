package io.rosecloud.auth.service;
import lombok.RequiredArgsConstructor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.auth.persistence.AuthCredentialMapper;
import io.rosecloud.auth.persistence.CredentialEntity;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.security.credential.AuthCredential;
import io.rosecloud.common.security.credential.PasswordPolicyValidator;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import io.rosecloud.common.security.session.SessionStore;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Auth-owned credential operations, backed directly by {@link AuthCredentialMapper} (the only
 * persistence mechanism). Enforces the shared password policy on every write and revokes active
 * sessions after a password change so a previously valid token cannot be reused. The system
 * service reaches this only through the credential API.
 */
@RequiredArgsConstructor
@Service
public class CredentialService {

    private final AuthCredentialMapper credentialMapper;
    private final PasswordEncoder passwordEncoder;
    private final SessionStore sessionStore;
    public Optional<AuthCredential> findByUserId(Long userId) {
        return Optional.ofNullable(credentialMapper.selectOne(byUserId(userId))).map(this::toModel);
    }

    public void setPassword(Long userId, String rawPassword) {
        PasswordPolicyValidator.validate(rawPassword);
        String hash = passwordEncoder.encode(rawPassword);
        CredentialEntity existing = credentialMapper.selectOne(byUserId(userId));
        if (existing == null) {
            CredentialEntity entity = new CredentialEntity();
            entity.setUserId(userId);
            entity.setPasswordHash(hash);
            entity.setPasswordChangedTime(LocalDateTime.now());
            entity.setAuthStatus(1);
            credentialMapper.insert(entity);
        } else {
            existing.setPasswordHash(hash);
            existing.setPasswordChangedTime(LocalDateTime.now());
            existing.setAuthStatus(1);
            credentialMapper.updateById(existing);
        }
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        AuthCredential credential = findByUserId(userId)
                .orElseThrow(() -> new BizException(SecurityErrorCode.BAD_CREDENTIALS));
        if (credential.passwordHash() == null
                || !passwordEncoder.matches(currentPassword, credential.passwordHash())) {
            throw new BizException(SecurityErrorCode.BAD_CREDENTIALS);
        }
        PasswordPolicyValidator.validateChange(currentPassword, newPassword);
        setPassword(userId, newPassword);
        sessionStore.revokeByUserId(userId);
    }

    private AuthCredential toModel(CredentialEntity entity) {
        return new AuthCredential(entity.getUserId(), entity.getPasswordHash(), entity.getPasswordChangedTime(),
                entity.getAuthStatus() != null && entity.getAuthStatus() == 1, entity.getLastLoginTime());
    }

    private LambdaQueryWrapper<CredentialEntity> byUserId(Long userId) {
        return new LambdaQueryWrapper<CredentialEntity>().eq(CredentialEntity::getUserId, userId);
    }
}
