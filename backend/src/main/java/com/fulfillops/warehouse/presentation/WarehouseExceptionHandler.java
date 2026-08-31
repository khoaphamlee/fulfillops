package com.fulfillops.warehouse.presentation;

import com.fulfillops.common.api.ApiErrorResponse;
import com.fulfillops.common.web.RequestIdFilter;
import com.fulfillops.warehouse.application.WarehouseCodeAlreadyExistsException;
import com.fulfillops.warehouse.application.WarehouseNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WarehouseExceptionHandler {

    @ExceptionHandler(WarehouseNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            WarehouseNotFoundException exception,
            HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "WAREHOUSE_NOT_FOUND", "Warehouse not found.", request);
    }

    @ExceptionHandler(WarehouseCodeAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleCodeConflict(
            WarehouseCodeAlreadyExistsException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.CONFLICT,
                "WAREHOUSE_CODE_CONFLICT",
                "Warehouse code already exists in this tenant.",
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
