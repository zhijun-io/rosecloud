package io.rosecloud.common.security.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Runtime representation of an authenticated principal.  Stored in the
 * security context after a successful login and carried inside JWTs.
 *
 * <p>Authorities are stored internally as strings and lazily wrapped into
 * {@link SimpleGrantedAuthority} objects so that Jackson serialization
 * (used over Feign between system and auth services) stays simple and
 * deterministic: the JSON wire format is {@code ["ROLE_admin", "system:user:list"]}.
 *
 * <p>A single programmatic constructor avoids ambiguity in overload resolution.
 * Jackson deserialisation uses the {@link #fromJson(Long, String, String, String,
 * boolean, UserPrincipal, List)} factory method.
 */
public class SecurityUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String username;
    private final String nickname;
    private final String password;
    private final boolean enabled;
    private final UserPrincipal userPrincipal;
    private final List<String> authorityStrings;

    // Lazily instantiated wrapper view — not serialized.
    private transient volatile Collection<GrantedAuthority> authoritiesView;

    /**
     * Programmatic constructor — used by authentication providers,
     * {@code UserDetailsService} implementations, and (via
     * {@link #fromJson}) Jackson.
     */
    public SecurityUser(Long userId, String username, String nickname, String password,
                        boolean enabled, UserPrincipal userPrincipal,
                        Collection<GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.password = password;
        this.enabled = enabled;
        this.userPrincipal = userPrincipal;
        this.authorityStrings = authorities != null
                ? authorities.stream().map(GrantedAuthority::getAuthority).toList()
                : List.of();
    }

    /**
     * Jackson factory — same contract as the programmatic constructor but
     * accepts raw authority strings as they arrive on the wire from Feign.
     */
    @JsonCreator
    public static SecurityUser fromJson(
            @JsonProperty("userId") Long userId,
            @JsonProperty("username") String username,
            @JsonProperty("nickname") String nickname,
            @JsonProperty("password") String password,
            @JsonProperty("enabled") boolean enabled,
            @JsonProperty("userPrincipal") UserPrincipal userPrincipal,
           @JsonProperty("authorities") List<String> authorityStrings) {
        List<GrantedAuthority> authorities = authorityStrings != null
                ? authorityStrings.stream().<GrantedAuthority>map(SimpleGrantedAuthority::new).toList()
                : List.of();
        return new SecurityUser(userId, username, nickname, password, enabled, userPrincipal,
                authorities);
    }

    // ---- UserDetails contract ----

    @Override
    @JsonIgnore
    public Collection<GrantedAuthority> getAuthorities() {
        if (authoritiesView == null) {
            synchronized (this) {
                if (authoritiesView == null) {
                    authoritiesView = authorityStrings.stream()
                            .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                            .toList();
                }
            }
        }
        return authoritiesView;
    }

    @JsonProperty("authorities")
    public List<String> getAuthorityStrings() {
        return authorityStrings;
    }

    @Override
    @JsonIgnore
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() { return true; }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() { return true; }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }

    // ---- Accessors exposed to downstream code ----

    public Long getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public UserPrincipal getUserPrincipal() { return userPrincipal; }
}
