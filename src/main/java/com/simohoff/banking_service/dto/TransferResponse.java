package com.simohoff.banking_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResponse(
        Long debitTransactionId,
        Long creditTransactionId,
        String fromIban,
        String toIban,
        BigDecimal amount,
        String reference,
        LocalDateTime timestamp) {
}