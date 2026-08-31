package com.fulfillops.user.presentation;

import com.fulfillops.common.api.ApiErrorResponse;
import com.fulfillops.common.web.RequestIdFilter;
import com.fulfillops.user.application.UserEmailAlreadyExistsException;
import com.fulfillops.user.application.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UserExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            UserNotFoundException exception,
            HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found.", request);
    }

    @ExceptionHandler(UserEmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailConflict(
            UserEmailAlreadyExistsException exception,
            HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "USER_EMAIL_CONFLICT", "User email already exists.", request);
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(
                code,
                message,
                status.value(),
                request.getRequestURI(),
                Instant.now(),
                RequestIdFilter.getRequestId(request),
                List.of());
        return ResponseEntity.status(status).body(body);
    }
}
