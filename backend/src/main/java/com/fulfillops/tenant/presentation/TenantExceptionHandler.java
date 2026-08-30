package com.fulfillops.tenant.presentation;

import com.fulfillops.common.api.ApiErrorResponse;
import com.fulfillops.common.web.RequestIdFilter;
import com.fulfillops.tenant.application.TenantCodeAlreadyExistsException;
import com.fulfillops.tenant.application.TenantNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = TenantController.class)
public class TenantExceptionHandler {

    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            TenantNotFoundException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.NOT_FOUND,
                "TENANT_NOT_FOUND",
                "Tenant not found.",
                request);
    }

    @ExceptionHandler(TenantCodeAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleCodeConflict(
            TenantCodeAlreadyExistsException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.CONFLICT,
                "TENANT_CODE_CONFLICT",
                "Tenant code already exists.",
                request);
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
