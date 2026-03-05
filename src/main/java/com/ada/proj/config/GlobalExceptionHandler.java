package com.ada.proj.config;

import com.ada.proj.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.validation.FieldError;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.enums.ErrorCode;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException e, HttpServletRequest req) {
        logWarn(e, req, 400, ErrorCode.BAD_REQUEST.name());
        return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.BAD_REQUEST.name(), e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(IllegalStateException e, HttpServletRequest req) {
        logWarn(e, req, 409, ErrorCode.CONFLICT.name());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ErrorCode.CONFLICT.name(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException e, HttpServletRequest req) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            if (fe == null) {
                continue;
            }
            String field = fe.getField();
            String message = fe.getDefaultMessage();
            if (field != null && !field.isBlank() && message != null && !message.isBlank()) {
                errors.putIfAbsent(field, message);
            }
        }

        String msg;
        if (!errors.isEmpty()) {
            Map.Entry<String, String> first = errors.entrySet().iterator().next();
            msg = first.getKey() + ": " + first.getValue();
        } else {
            msg = e.getBindingResult().getAllErrors().stream().findFirst()
                    .map(err -> err.getDefaultMessage()).orElse("요청 값이 유효하지 않습니다");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("errors", errors);

        logWarn(e, req, 400, ErrorCode.VALIDATION_ERROR.name());
        return ResponseEntity.badRequest().body(ApiResponse.errorWithData(ErrorCode.VALIDATION_ERROR.name(), msg, data));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException e, HttpServletRequest req) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (e != null && e.getConstraintViolations() != null) {
            for (ConstraintViolation<?> v : e.getConstraintViolations()) {
                if (v == null) {
                    continue;
                }
                String path = v.getPropertyPath() == null ? null : v.getPropertyPath().toString();
                String message = v.getMessage();
                if (path != null && !path.isBlank() && message != null && !message.isBlank()) {
                    errors.putIfAbsent(path, message);
                }
            }
        }

        String msg;
        if (!errors.isEmpty()) {
            Map.Entry<String, String> first = errors.entrySet().iterator().next();
            msg = first.getKey() + ": " + first.getValue();
        } else {
            msg = "요청 값이 유효하지 않습니다";
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("errors", errors);

        logWarn(e, req, 400, ErrorCode.VALIDATION_ERROR.name());
        return ResponseEntity.badRequest().body(ApiResponse.errorWithData(ErrorCode.VALIDATION_ERROR.name(), msg, data));
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthenticated(UnauthenticatedException e, HttpServletRequest req) {
        logWarn(e, req, 401, ErrorCode.UNAUTHENTICATED.name());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.UNAUTHENTICATED.name(), e.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException e, HttpServletRequest req) {
        logWarn(e, req, 403, ErrorCode.FORBIDDEN.name());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.FORBIDDEN.name(), e.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(UserNotFoundException e, HttpServletRequest req) {
        logWarn(e, req, 404, ErrorCode.USER_NOT_FOUND.name());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorCode.USER_NOT_FOUND.name(), e.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(EntityNotFoundException e, HttpServletRequest req) {
        logWarn(e, req, 404, ErrorCode.BAD_REQUEST.name());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorCode.BAD_REQUEST.name(), e.getMessage()));
    }

    @ExceptionHandler({InvalidCredentialsException.class, TokenInvalidException.class, TokenExpiredException.class})
    public ResponseEntity<ApiResponse<Void>> handleAuthFailures(RuntimeException e, HttpServletRequest req) {
        String code
                = e instanceof InvalidCredentialsException ? ErrorCode.INVALID_PASSWORD.name()
                        : e instanceof TokenExpiredException ? ErrorCode.TOKEN_EXPIRED.name()
                                : e instanceof TokenInvalidException ? ErrorCode.TOKEN_INVALID.name()
                                        : ErrorCode.AUTH_FAILURE.name();
        logWarn(e, req, 401, code);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(code, e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurity(SecurityException e, HttpServletRequest req) {
        logWarn(e, req, 403, ErrorCode.FORBIDDEN.name());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.FORBIDDEN.name(), e.getMessage()));
    }

    /* ================================
       ★ BannedUserException 처리 추가
       ================================ */
    @ExceptionHandler(BannedUserException.class)
    public ResponseEntity<ApiResponse<Void>> handleBanned(BannedUserException e, HttpServletRequest req) {
        logWarn(e, req, 403, ErrorCode.USER_BANNED.name());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.USER_BANNED.name(), e.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException e, HttpServletRequest req) {
        logWarn(e, req, 404, ErrorCode.NOT_FOUND.name());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorCode.NOT_FOUND.name(), "Not found"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e, HttpServletRequest req) {
        logError(e, req, 500, ErrorCode.INTERNAL_ERROR.name());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.name(), "Internal server error"));
    }

    private void logWarn(Exception e, HttpServletRequest req, int status, String code) {
        String rid = MDC.get("requestId");
        String path = req != null ? req.getRequestURI() : "";
        LoggerFactory.getLogger(GlobalExceptionHandler.class)
                .warn("에러 요청: id={} code={} status={} path={} msg={}", rid, code, status, path, e.getMessage());
    }

    private void logError(Exception e, HttpServletRequest req, int status, String code) {
        String rid = MDC.get("requestId");
        String path = req != null ? req.getRequestURI() : "";
        log.error("서버 오류: id={} code={} status={} path={}", rid, code, status, path, e);
    }

    @ExceptionHandler(AlreadyBannedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAlreadyBanned(AlreadyBannedException e, HttpServletRequest req) {
        logWarn(e, req, 400, ErrorCode.ALREADY_BANNED.name());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.ALREADY_BANNED.name(), e.getMessage()));
    }

    @ExceptionHandler(BanExpiresInvalidException.class)
    public ResponseEntity<ApiResponse<Void>> handleBanExpiresInvalid(
            BanExpiresInvalidException e, HttpServletRequest req) {

        logWarn(e, req, 400, ErrorCode.BAN_EXPIRES_INVALID.name());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.BAN_EXPIRES_INVALID.name(), e.getMessage()));
    }
}
