package com.fulfillops.sku.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSkuRequest(
        @NotBlank
        @Size(max = 63)
        @Pattern(regexp = "^[A-Za-z0-9]+([-_][A-Za-z0-9]+)*$")
        String code,
        @NotBlank
        @Size(max = 255)
        String name) {
}
