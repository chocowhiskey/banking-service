package com.simohoff.banking_service.dto;

import com.simohoff.banking_service.domain.Account;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        String iban,
        String ownerName,
        BigDecimal balance,
        LocalDateTime createdAt) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getIban(),
                account.getOwnerName(),
                account.getBalance(),
                account.getCreatedAt());
    }
}