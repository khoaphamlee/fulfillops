package com.fulfillops.warehouse.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateWarehouseRequest(
        @NotBlank
        @Size(max = 63)
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$")
        String code,
        @NotBlank
        @Size(max = 255)
        String name) {
}
