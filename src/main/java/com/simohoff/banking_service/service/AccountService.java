package com.simohoff.banking_service.service;

import com.simohoff.banking_service.domain.Account;
import com.simohoff.banking_service.domain.Transaction;
import com.simohoff.banking_service.exception.AccountNotFoundException;
import com.simohoff.banking_service.repository.AccountRepository;
import com.simohoff.banking_service.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository,
            TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Erstellt ein neues Konto.
     */
    @Transactional
    public Account createAccount(String iban, String ownerName) {
        if (accountRepository.existsByIban(iban)) {
            throw new IllegalArgumentException("Account with IBAN " + iban + " already exists");
        }

        Account account = new Account(iban, ownerName);
        return accountRepository.save(account);
    }

    /**
     * Gibt ein Konto anhand der IBAN zurück.
     */
    @Transactional(readOnly = true)
    public Account getAccount(String iban) {
        return accountRepository.findByIban(iban)
                .orElseThrow(() -> new AccountNotFoundException(iban));
    }

    /**
     * Bucht einen Betrag vom Konto ab.
     * Wichtig: Die gesamte Operation ist atomar!
     */
    @Transactional
    public Transaction debit(String iban, BigDecimal amount, String reference) {
        Account account = getAccount(iban);

        // Domain-Logik validiert automatisch!
        Transaction transaction = account.debit(amount, reference);

        // Beide Änderungen in EINER Transaktion
        accountRepository.save(account);
        transactionRepository.save(transaction);

        return transaction;
    }

    /**
     * Bucht einen Betrag auf das Konto ein.
     */
    @Transactional
    public Transaction credit(String iban, BigDecimal amount, String reference) {
        Account account = getAccount(iban);

        Transaction transaction = account.credit(amount, reference);

        accountRepository.save(account);
        transactionRepository.save(transaction);

        return transaction;
    }

    /**
     * Gibt alle Transaktionen eines Kontos zurück.
     */
    @Transactional(readOnly = true)
    public List<Transaction> getTransactions(String iban) {
        // Prüft erst ob Account existiert
        if (!accountRepository.existsByIban(iban)) {
            throw new AccountNotFoundException(iban);
        }

        return transactionRepository.findByAccountIbanOrderByTimestampDesc(iban);
    }
}