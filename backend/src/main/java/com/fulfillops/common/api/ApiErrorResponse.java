package com.fulfillops.common.api;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        int status,
        String path,
        Instant timestamp,
        String requestId,
        List<FieldValidationError> fieldErrors) {
}
