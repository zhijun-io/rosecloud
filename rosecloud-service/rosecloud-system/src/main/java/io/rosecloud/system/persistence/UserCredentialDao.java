package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * DAO for user credential records (activation tokens).
 * Does not extend {@code MyBatisDao} as {@link UserCredentialEntity} has no domain abstraction.
 * This DAO is restricted to activation-token state; password operations go through CredentialApi.
 */
@Repository
public class UserCredentialDao {

    private final UserCredentialMapper mapper;

    public UserCredentialDao(UserCredentialMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<UserCredentialEntity> findByActivateToken(String activateToken) {
        if (activateToken == null || activateToken.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<UserCredentialEntity>()
                        .eq(UserCredentialEntity::getActivateToken, activateToken)));
    }

    public Optional<UserCredentialEntity> findByUserId(Long userId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<UserCredentialEntity>()
                        .eq(UserCredentialEntity::getUserId, userId)));
    }

    public void update(UserCredentialEntity entity) {
        mapper.updateById(entity);
    }
}
