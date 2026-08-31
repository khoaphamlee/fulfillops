package com.fulfillops.warehouse.location.presentation;

import com.fulfillops.common.api.ApiErrorResponse;
import com.fulfillops.common.web.RequestIdFilter;
import com.fulfillops.warehouse.location.application.WarehouseAisleCodeAlreadyExistsException;
import com.fulfillops.warehouse.location.application.WarehouseAisleNotFoundException;
import com.fulfillops.warehouse.location.application.WarehouseBinCodeAlreadyExistsException;
import com.fulfillops.warehouse.location.application.WarehouseBinNotFoundException;
import com.fulfillops.warehouse.location.application.WarehouseRackCodeAlreadyExistsException;
import com.fulfillops.warehouse.location.application.WarehouseRackNotFoundException;
import com.fulfillops.warehouse.location.application.WarehouseZoneCodeAlreadyExistsException;
import com.fulfillops.warehouse.location.application.WarehouseZoneNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {
        WarehouseZoneController.class, WarehouseAisleController.class,
        WarehouseRackController.class, WarehouseBinController.class
})
public class WarehouseLocationExceptionHandler {
    @ExceptionHandler(WarehouseZoneNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> zoneNotFound(HttpServletRequest request) { return error(HttpStatus.NOT_FOUND, "ZONE_NOT_FOUND", "Zone not found.", request); }
    @ExceptionHandler(WarehouseZoneCodeAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> zoneConflict(HttpServletRequest request) { return error(HttpStatus.CONFLICT, "ZONE_CODE_CONFLICT", "Zone code already exists in this warehouse.", request); }
    @ExceptionHandler(WarehouseAisleNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> aisleNotFound(HttpServletRequest request) { return error(HttpStatus.NOT_FOUND, "AISLE_NOT_FOUND", "Aisle not found.", request); }
    @ExceptionHandler(WarehouseAisleCodeAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> aisleConflict(HttpServletRequest request) { return error(HttpStatus.CONFLICT, "AISLE_CODE_CONFLICT", "Aisle code already exists in this zone.", request); }
    @ExceptionHandler(WarehouseRackNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> rackNotFound(HttpServletRequest request) { return error(HttpStatus.NOT_FOUND, "RACK_NOT_FOUND", "Rack not found.", request); }
    @ExceptionHandler(WarehouseRackCodeAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> rackConflict(HttpServletRequest request) { return error(HttpStatus.CONFLICT, "RACK_CODE_CONFLICT", "Rack code already exists in this aisle.", request); }
    @ExceptionHandler(WarehouseBinNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> binNotFound(HttpServletRequest request) { return error(HttpStatus.NOT_FOUND, "BIN_NOT_FOUND", "Bin not found.", request); }
    @ExceptionHandler(WarehouseBinCodeAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> binConflict(HttpServletRequest request) { return error(HttpStatus.CONFLICT, "BIN_CODE_CONFLICT", "Bin code already exists in this rack.", request); }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message, HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(code, message, status.value(), request.getRequestURI(), Instant.now(), RequestIdFilter.getRequestId(request), List.of());
        return ResponseEntity.status(status).body(body);
    }
}
