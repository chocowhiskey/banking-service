package com.simohoff.banking_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "Source IBAN is required") String fromIban,

        @NotBlank(message = "Destination IBAN is required") String toIban,

        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be positive") BigDecimal amount,

        String reference) {
}
