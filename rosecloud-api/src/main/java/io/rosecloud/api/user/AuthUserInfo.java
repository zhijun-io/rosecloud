package io.rosecloud.api.user;

import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.model.UserPrincipal;

import java.util.List;

/**
 * Authentication-side projection of a user record.
 *
 * <p>This type deliberately does not implement {@code UserDetails}; it exists
 * only to move the password hash from the system service to the auth service
 * without tripping security-specific Jackson mixins.
 */
public record AuthUserInfo(Long userId, String username, String nickname, String encodedPassword,
                           boolean enabled, UserPrincipal userPrincipal, List<String> authorities) {

    public static AuthUserInfo from(SecurityUser user) {
        return new AuthUserInfo(
                user.getUserId(),
                user.getUsername(),
                user.getNickname(),
                user.getPassword(),
                user.isEnabled(),
                user.getUserPrincipal(),
                user.getAuthorityStrings()
        );
    }
}
