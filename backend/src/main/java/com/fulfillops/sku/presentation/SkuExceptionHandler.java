package com.fulfillops.sku.presentation;

import com.fulfillops.common.api.ApiErrorResponse;
import com.fulfillops.common.web.RequestIdFilter;
import com.fulfillops.sku.application.SkuCodeAlreadyExistsException;
import com.fulfillops.sku.application.SkuNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = SkuController.class)
public class SkuExceptionHandler {

    @ExceptionHandler(SkuNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "SKU_NOT_FOUND", "SKU not found.", request);
    }

    @ExceptionHandler(SkuCodeAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleCodeConflict(HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "SKU_CODE_CONFLICT", "SKU code already exists in this tenant.", request);
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
