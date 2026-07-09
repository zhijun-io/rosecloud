package io.rosecloud.starter.security.auth;

import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.token.RawAccessJwtToken;

public class RefreshAuthenticationToken extends AbstractJwtAuthenticationToken {

    private static final long serialVersionUID = 1L;

    public RefreshAuthenticationToken(RawAccessJwtToken unsafeToken) {
        super(unsafeToken);
    }

    public RefreshAuthenticationToken(SecurityUser securityUser) {
        super(securityUser);
    }
}
