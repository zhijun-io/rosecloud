package io.rosecloud.system.service;

import io.rosecloud.api.user.UserPasswordUpdateRequest;
import io.rosecloud.api.user.AuthUserInfo;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.core.model.PageResult;

import java.time.LocalDateTime;

import io.rosecloud.system.domain.User;
import io.rosecloud.system.service.dto.ChangePasswordRequest;
import io.rosecloud.system.service.dto.UserCreateRequest;
import io.rosecloud.system.service.dto.UserProfile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

import java.util.Optional;

public interface UserService {

    Long create(UserCreateRequest request);

    Long createWithHash(String username, String passwordHash, String nickname, String tenantId);

    Long createWithoutPassword(String username, String nickname, String tenantId);

    PageResult<User> page(long current, long size, String keyword);

    User get(Long id);

    void delete(Long id);

    SecurityUser loadByUsername(String username);

    Optional<AuthUserInfo> loadAuthInfoByUsername(String username);

    void changePassword(ChangePasswordRequest request);

    void updatePassword(Long userId, UserPasswordUpdateRequest request);

    void assignRoles(Long userId, List<Long> roleIds);

    List<Long> findRoleIdsByUserId(Long userId);

    UserProfile me();
}
