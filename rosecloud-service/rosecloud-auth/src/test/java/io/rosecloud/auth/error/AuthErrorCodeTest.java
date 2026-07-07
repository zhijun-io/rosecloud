package io.rosecloud.auth.error;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthErrorCodeTest {

    @Test
    void codeDerivesFromModulePrefixAndName() {
        assertThat(AuthErrorCode.INVALID_TOKEN.code()).isEqualTo("auth.invalid_token");
    }

    @Test
    void codeDerivesForConstantWithBody() {
        // ACCOUNT_LOCKED has an anonymous subclass body; modulePrefix must still
        // resolve to the enclosing AuthErrorCode, not the empty anonymous name.
        assertThat(AuthErrorCode.ACCOUNT_LOCKED.code()).isEqualTo("auth.account_locked");
        assertThat(AuthErrorCode.ACCOUNT_LOCKED.httpStatus()).isEqualTo(423);
    }
}
