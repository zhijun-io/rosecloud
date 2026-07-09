package io.rosecloud.starter.security.auth;

import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.token.RawAccessJwtToken;

public class JwtAuthenticationToken extends AbstractJwtAuthenticationToken {

    private static final long serialVersionUID = 1L;

    public JwtAuthenticationToken(RawAccessJwtToken unsafeToken) {
        super(unsafeToken);
    }

    public JwtAuthenticationToken(SecurityUser securityUser) {
        super(securityUser);
    }
}
