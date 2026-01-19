package com.simohoff.banking_service.dto;

import com.simohoff.banking_service.domain.Transaction;
import com.simohoff.banking_service.domain.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        BigDecimal amount,
        TransactionType type,
        String reference,
        LocalDateTime timestamp) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getReference(),
                transaction.getTimestamp());
    }
}