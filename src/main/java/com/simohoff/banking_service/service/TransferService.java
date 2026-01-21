package com.simohoff.banking_service.service;

import com.simohoff.banking_service.domain.Account;
import com.simohoff.banking_service.domain.Transaction;
import com.simohoff.banking_service.dto.TransferResponse;
import com.simohoff.banking_service.exception.AccountNotFoundException;
import com.simohoff.banking_service.repository.AccountRepository;
import com.simohoff.banking_service.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransferService(AccountRepository accountRepository,
            TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Überweist Geld zwischen zwei Konten.
     * WICHTIG: Beide Buchungen müssen atomar erfolgen!
     * 
     * @throws AccountNotFoundException wenn eines der Konten nicht existiert
     * @throws IllegalArgumentException wenn Absender nicht genug Guthaben hat
     */
    @Transactional
    public TransferResponse transfer(String fromIban, String toIban, BigDecimal amount, String reference) {
        // Validierung
        if (fromIban.equals(toIban)) {
            throw new IllegalArgumentException("Cannot transfer to same account");
        }

        // Beide Konten laden
        Account fromAccount = accountRepository.findByIban(fromIban)
                .orElseThrow(() -> new AccountNotFoundException(fromIban));

        Account toAccount = accountRepository.findByIban(toIban)
                .orElseThrow(() -> new AccountNotFoundException(toIban));

        // Domain-Logik: Geld abbuchen und einzahlen
        String transferReference = reference != null ? reference : "Transfer";

        Transaction debitTransaction = fromAccount.debit(amount, "Transfer to " + toIban + ": " + transferReference);
        Transaction creditTransaction = toAccount.credit(amount,
                "Transfer from " + fromIban + ": " + transferReference);

        // Alles speichern (in EINER Transaktion!)
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        transactionRepository.save(debitTransaction);
        transactionRepository.save(creditTransaction);

        return new TransferResponse(
                debitTransaction.getId(),
                creditTransaction.getId(),
                fromIban,
                toIban,
                amount,
                transferReference,
                debitTransaction.getTimestamp());
    }
}