package com.fulfillops.warehouse.location.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateWarehouseBinRequest(
        @NotBlank @Size(max = 63) @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$") String code) {
}
