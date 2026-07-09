package io.rosecloud.starter.web.exception;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.error.CommonErrorCode;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

/**
 * Translates exceptions to a failed {@link ApiResponse}, keeping a consistent
 * envelope across all services.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBiz(BizException ex) {
        return respond(HttpStatus.valueOf(ex.getErrorCode().httpStatus()),
                ApiResponse.failure(ex.getErrorCode().code(), ex.getMessage()));
    }

    @ExceptionHandler({
            AuthenticationException.class,
            AccessDeniedException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleSecurity(Exception ex) {
        SecurityErrorCode errorCode = ex instanceof AccessDeniedException
                ? SecurityErrorCode.FORBIDDEN
                : SecurityErrorCode.UNAUTHORIZED;
        return respond(HttpStatus.valueOf(errorCode.httpStatus()),
                ApiResponse.failure(errorCode.code(), errorCode.message()));
    }

    @ExceptionHandler({
            BindException.class,
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        log.warn("parameter validation failed: {}", ex.getMessage());
        String detail = ex.getMessage();
        if (detail != null && detail.length() > 500) {
            detail = detail.substring(0, 500);
        }
        return respond(HttpStatus.BAD_REQUEST, ApiResponse.failure(CommonErrorCode.PARAM_INVALID.code(), detail));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAny(Exception ex) {
        log.error("unhandled exception", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ApiResponse.failure(CommonErrorCode.INTERNAL_ERROR));
    }

    private ResponseEntity<ApiResponse<Void>> respond(HttpStatus status, ApiResponse<Void> body) {
        return ResponseEntity.status(status).body(body);
    }
}
