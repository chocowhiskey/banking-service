package com.simohoff.banking_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 22)
    private String iban;

    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Account(String iban, String ownerName) {
        this.iban = iban;
        this.ownerName = ownerName;
        this.balance = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
    }

    // ========== DOMAIN LOGIC (NEU!) ==========

    /**
     * Bucht einen Betrag vom Konto ab.
     * 
     * @throws IllegalArgumentException wenn Betrag negativ oder Kontostand nicht
     *                                  ausreicht
     */
    public Transaction debit(BigDecimal amount, String reference) {
        validateAmount(amount);

        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient funds. Balance: " + this.balance + ", Required: " + amount);
        }

        this.balance = this.balance.subtract(amount);
        return new Transaction(amount, TransactionType.DEBIT, reference, this);
    }

    /**
     * Bucht einen Betrag auf das Konto ein.
     * 
     * @throws IllegalArgumentException wenn Betrag negativ
     */
    public Transaction credit(BigDecimal amount, String reference) {
        validateAmount(amount);

        this.balance = this.balance.add(amount);
        return new Transaction(amount, TransactionType.CREDIT, reference, this);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}