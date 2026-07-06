package io.rosecloud.common.core.error;

/**
 * Stable error-code contract. Services declare their own enums implementing
 * this so business errors carry a machine-readable code plus a default message.
 *
 * <p>Code format: {@code {MODULE}{TYPE}{SEQ}}, e.g. {@code USRB001} — see
 * AGENTS.md. Module {@code CMM} is reserved for common infrastructure.
 */
public interface ErrorCode {

    String code();

    String message();
}
