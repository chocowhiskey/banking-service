package com.simohoff.banking_service.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class AccountTest {

    @Test
    void shouldCreditAccount() {
        // Given
        Account account = new Account("DE123", "Max");

        // When
        Transaction transaction = account.credit(new BigDecimal("100"), "Gehalt");

        // Then
        assertThat(account.getBalance()).isEqualByComparingTo("100");
        assertThat(transaction.getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(transaction.getAmount()).isEqualByComparingTo("100");
    }

    @Test
    void shouldDebitAccount() {
        // Given
        Account account = new Account("DE123", "Max");
        account.credit(new BigDecimal("100"), "Initial");

        // When
        Transaction transaction = account.debit(new BigDecimal("30"), "Einkauf");

        // Then
        assertThat(account.getBalance()).isEqualByComparingTo("70");
        assertThat(transaction.getType()).isEqualTo(TransactionType.DEBIT);
    }

    @Test
    void shouldNotAllowNegativeBalance() {
        // Given
        Account account = new Account("DE123", "Max");
        account.credit(new BigDecimal("50"), "Initial");

        // When / Then
        assertThatThrownBy(() -> account.debit(new BigDecimal("100"), "Zu viel"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    void shouldNotAllowNegativeAmount() {
        // Given
        Account account = new Account("DE123", "Max");

        // When / Then
        assertThatThrownBy(() -> account.credit(new BigDecimal("-10"), "Negativ"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");
    }
}