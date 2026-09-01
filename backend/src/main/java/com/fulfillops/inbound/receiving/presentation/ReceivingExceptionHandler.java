package com.fulfillops.inbound.receiving.presentation;

import com.fulfillops.common.api.ApiErrorResponse;
import com.fulfillops.common.web.RequestIdFilter;
import com.fulfillops.inbound.receiving.application.ReceivingDuplicateLineException;
import com.fulfillops.inbound.receiving.application.ReceivingPlannedLineNotFoundException;
import com.fulfillops.inbound.receiving.application.ReceivingQuantityExceedsExpectedException;
import com.fulfillops.inbound.receiving.application.ReceivingReceiptNotFoundException;
import com.fulfillops.inbound.receiving.application.ReceivingIdempotencyKeyReusedWithDifferentRequestException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ReceivingReceiptController.class)
public class ReceivingExceptionHandler {
    @ExceptionHandler(ReceivingIdempotencyKeyRequiredException.class)
    public ResponseEntity<ApiErrorResponse> idempotencyKeyRequired(HttpServletRequest request) { return error(HttpStatus.BAD_REQUEST, "RECEIVING_IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key header is required.", request); }
    @ExceptionHandler(ReceivingIdempotencyKeyInvalidException.class)
    public ResponseEntity<ApiErrorResponse> idempotencyKeyInvalid(HttpServletRequest request) { return error(HttpStatus.BAD_REQUEST, "RECEIVING_IDEMPOTENCY_KEY_INVALID", "Idempotency-Key header is invalid.", request); }
    @ExceptionHandler(ReceivingIdempotencyKeyReusedWithDifferentRequestException.class)
    public ResponseEntity<ApiErrorResponse> idempotencyKeyReusedWithDifferentRequest(HttpServletRequest request) { return error(HttpStatus.CONFLICT, "RECEIVING_IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST", "Idempotency-Key was already used with a different receiving request.", request); }
    @ExceptionHandler(ReceivingReceiptNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> receiptNotFound(HttpServletRequest request) { return error(HttpStatus.NOT_FOUND, "RECEIVING_RECEIPT_NOT_FOUND", "Receiving receipt not found.", request); }
    @ExceptionHandler(ReceivingPlannedLineNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> plannedLineNotFound(HttpServletRequest request) { return error(HttpStatus.NOT_FOUND, "RECEIVING_PLANNED_LINE_NOT_FOUND", "Planned inbound line not found.", request); }
    @ExceptionHandler(ReceivingDuplicateLineException.class)
    public ResponseEntity<ApiErrorResponse> duplicateLine(HttpServletRequest request) { return error(HttpStatus.CONFLICT, "RECEIVING_DUPLICATE_LINE", "A planned line can appear only once in a receiving receipt.", request); }
    @ExceptionHandler(ReceivingQuantityExceedsExpectedException.class)
    public ResponseEntity<ApiErrorResponse> quantityExceedsExpected(HttpServletRequest request) { return error(HttpStatus.CONFLICT, "RECEIVING_QUANTITY_EXCEEDS_EXPECTED", "Received quantity exceeds the remaining expected quantity.", request); }
    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message, HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(code, message, status.value(), request.getRequestURI(), Instant.now(), RequestIdFilter.getRequestId(request), List.of());
        return ResponseEntity.status(status).body(body);
    }
}
