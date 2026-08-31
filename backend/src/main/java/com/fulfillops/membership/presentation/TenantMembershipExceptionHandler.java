package com.fulfillops.membership.presentation;

import com.fulfillops.common.api.ApiErrorResponse;
import com.fulfillops.common.web.RequestIdFilter;
import com.fulfillops.membership.application.TenantMembershipAlreadyExistsException;
import com.fulfillops.membership.application.TenantMembershipNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = TenantMembershipController.class)
public class TenantMembershipExceptionHandler {

    @ExceptionHandler(TenantMembershipNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            TenantMembershipNotFoundException exception,
            HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "MEMBERSHIP_NOT_FOUND", "Tenant membership not found.", request);
    }

    @ExceptionHandler(TenantMembershipAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            TenantMembershipAlreadyExistsException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.CONFLICT,
                "TENANT_MEMBERSHIP_CONFLICT",
                "Tenant membership already exists.",
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
