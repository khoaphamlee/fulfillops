package com.fulfillops.inbound.presentation;

import com.fulfillops.common.api.ApiErrorResponse;
import com.fulfillops.common.web.RequestIdFilter;
import com.fulfillops.inbound.application.InboundShipmentDuplicateSkuLineException;
import com.fulfillops.inbound.application.InboundShipmentNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class InboundShipmentExceptionHandler {
    @ExceptionHandler(InboundShipmentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(HttpServletRequest request) { return error(HttpStatus.NOT_FOUND, "INBOUND_SHIPMENT_NOT_FOUND", "Inbound shipment not found.", request); }
    @ExceptionHandler(InboundShipmentDuplicateSkuLineException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateSkuLine(HttpServletRequest request) { return error(HttpStatus.CONFLICT, "INBOUND_SHIPMENT_DUPLICATE_SKU_LINE", "A SKU can appear only once in an inbound shipment.", request); }
    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message, HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(code, message, status.value(), request.getRequestURI(), Instant.now(), RequestIdFilter.getRequestId(request), List.of());
        return ResponseEntity.status(status).body(body);
    }
}
