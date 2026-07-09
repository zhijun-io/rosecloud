package io.rosecloud.common.security.token;

import io.rosecloud.common.security.model.SecurityUser;

/**
 * Strategy for issuing authentication token pairs.
 *
 * <p>This interface lives in common-security so that consuming modules
 * (e.g. system) can request token creation through a compile-time-safe
 * contract without depending on the concrete signing implementation.</p>
 *
 * <p>The starter-security module provides the concrete {@code JwtTokenFactory}
 * bean that implements this contract.</p>
 */
public interface TokenFactory {

    /**
     * Create an access + refresh token pair for the given authenticated user.
     */
    JwtPair createTokenPair(SecurityUser user);

    /**
     * Number of seconds until the access token expires. Used by the login
     * response to inform the client about token lifetime.
     */
    long getAccessTokenExpirationSeconds();
}
