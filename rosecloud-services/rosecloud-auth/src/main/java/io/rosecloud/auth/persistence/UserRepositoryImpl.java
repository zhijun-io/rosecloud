package io.rosecloud.auth.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.auth.domain.AuthUser;
import io.rosecloud.auth.domain.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    public UserRepositoryImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public Optional<AuthUser> findByUsername(String username) {
        UserPO po = userMapper.selectOne(
                new LambdaQueryWrapper<UserPO>().eq(UserPO::getUsername, username));
        return Optional.ofNullable(po).map(this::toDomain);
    }

    private AuthUser toDomain(UserPO po) {
        return new AuthUser(po.getId(), po.getUsername(), po.getPassword(),
                po.getStatus(), po.getTenantId());
    }
}
