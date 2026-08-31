package com.fulfillops.user.presentation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank
        @Email
        @Size(max = 254)
        String email,
        @NotBlank
        @Size(max = 255)
        String displayName) {
}
