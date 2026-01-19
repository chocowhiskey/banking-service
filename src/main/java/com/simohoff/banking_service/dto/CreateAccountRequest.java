package com.simohoff.banking_service.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAccountRequest(
        @NotBlank(message = "IBAN is required") String iban,

        @NotBlank(message = "Owner name is required") String ownerName) {
}