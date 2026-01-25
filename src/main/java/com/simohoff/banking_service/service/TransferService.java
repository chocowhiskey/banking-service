package com.simohoff.banking_service.service;

import com.simohoff.banking_service.domain.Account;
import com.simohoff.banking_service.domain.Transaction;
import com.simohoff.banking_service.dto.TransferResponse;
import com.simohoff.banking_service.exception.AccountNotFoundException;
import com.simohoff.banking_service.repository.AccountRepository;
import com.simohoff.banking_service.repository.TransactionRepository;
import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
         * Überweist Geld zwischen zwei Konten mit Retry bei Optimistic Lock Failures.
         */
        public TransferResponse transfer(String fromIban, String toIban, BigDecimal amount, String reference) {
                int maxRetries = 3;
                int attempt = 0;

                while (attempt < maxRetries) {
                        try {
                                return performTransfer(fromIban, toIban, amount, reference);
                        } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
                                attempt++;
                                if (attempt >= maxRetries) {
                                        throw new RuntimeException(
                                                        "Transfer failed after " + maxRetries
                                                                        + " retries due to concurrent modifications",
                                                        e);
                                }
                                // Kurz warten vor erneutem Versuch
                                try {
                                        Thread.sleep(10 * attempt); // Exponentielles Backoff
                                } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        throw new RuntimeException("Transfer interrupted", ie);
                                }
                        }
                }

                throw new RuntimeException("Transfer failed");
        }

        @Transactional
        private TransferResponse performTransfer(String fromIban, String toIban, BigDecimal amount, String reference) {
                // Validierung
                if (fromIban.equals(toIban)) {
                        throw new IllegalArgumentException("Cannot transfer to same account");
                }

                // Beide Konten laden
                Account fromAccount = accountRepository.findByIban(fromIban)
                                .orElseThrow(() -> new AccountNotFoundException(fromIban));

                Account toAccount = accountRepository.findByIban(toIban)
                                .orElseThrow(() -> new AccountNotFoundException(toIban));

                // Domain-Logik
                String transferReference = reference != null ? reference : "Transfer";

                Transaction debitTransaction = fromAccount.debit(amount,
                                "Transfer to " + toIban + ": " + transferReference);
                Transaction creditTransaction = toAccount.credit(amount,
                                "Transfer from " + fromIban + ": " + transferReference);

                // Speichern (Version wird automatisch geprüft!)
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