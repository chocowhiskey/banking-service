package com.simohoff.banking_service.service;

import com.simohoff.banking_service.domain.Account;
import com.simohoff.banking_service.dto.TransferResponse;
import com.simohoff.banking_service.exception.AccountNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class TransferServiceTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountService accountService;

    private String account1Iban;
    private String account2Iban;

    @BeforeEach
    void setUp() {
        account1Iban = "DE111";
        account2Iban = "DE222";

        accountService.createAccount(account1Iban, "Alice");
        accountService.createAccount(account2Iban, "Bob");

        // Alice hat 1000 EUR
        accountService.credit(account1Iban, new BigDecimal("1000"), "Initial");
    }

    @Test
    void shouldTransferBetweenAccounts() {
        // When
        TransferResponse response = transferService.transfer(
                account1Iban,
                account2Iban,
                new BigDecimal("300"),
                "Testüberweisung");

        // Then
        assertThat(response.fromIban()).isEqualTo(account1Iban);
        assertThat(response.toIban()).isEqualTo(account2Iban);
        assertThat(response.amount()).isEqualByComparingTo("300");

        // Konten prüfen
        Account alice = accountService.getAccount(account1Iban);
        Account bob = accountService.getAccount(account2Iban);

        assertThat(alice.getBalance()).isEqualByComparingTo("700"); // 1000 - 300
        assertThat(bob.getBalance()).isEqualByComparingTo("300");
    }

    @Test
    void shouldNotTransferWhenInsufficientFunds() {
        // When / Then
        assertThatThrownBy(
                () -> transferService.transfer(account1Iban, account2Iban, new BigDecimal("2000"), "Zu viel"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient funds");

        // Konten sollten unverändert sein (Rollback!)
        Account alice = accountService.getAccount(account1Iban);
        Account bob = accountService.getAccount(account2Iban);

        assertThat(alice.getBalance()).isEqualByComparingTo("1000");
        assertThat(bob.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void shouldNotTransferToSameAccount() {
        // When / Then
        assertThatThrownBy(() -> transferService.transfer(account1Iban, account1Iban, new BigDecimal("100"), "Same"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same account");
    }

    @Test
    void shouldNotTransferWhenAccountNotFound() {
        // When / Then
        assertThatThrownBy(() -> transferService.transfer(account1Iban, "NOTEXISTING", new BigDecimal("100"), "Test"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void shouldCreateTwoTransactions() {
        // When
        transferService.transfer(account1Iban, account2Iban, new BigDecimal("100"), "Test");

        // Then
        var aliceTransactions = accountService.getTransactions(account1Iban);
        var bobTransactions = accountService.getTransactions(account2Iban);

        // Alice: Initial Credit + Transfer Debit
        assertThat(aliceTransactions).hasSize(2);

        // Bob: Transfer Credit
        assertThat(bobTransactions).hasSize(1);
    }
}