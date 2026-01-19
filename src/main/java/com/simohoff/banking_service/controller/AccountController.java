package com.simohoff.banking_service.controller;

import com.simohoff.banking_service.domain.Account;
import com.simohoff.banking_service.domain.Transaction;
import com.simohoff.banking_service.dto.*;
import com.simohoff.banking_service.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * POST /api/accounts
     * Erstellt ein neues Konto
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.createAccount(request.iban(), request.ownerName());
        return AccountResponse.from(account);
    }

    /**
     * GET /api/accounts/{iban}
     * Gibt ein Konto zurück
     */
    @GetMapping("/{iban}")
    public AccountResponse getAccount(@PathVariable String iban) {
        Account account = accountService.getAccount(iban);
        return AccountResponse.from(account);
    }

    /**
     * POST /api/accounts/{iban}/credit
     * Bucht Geld auf ein Konto ein
     */
    @PostMapping("/{iban}/credit")
    public TransactionResponse credit(
            @PathVariable String iban,
            @Valid @RequestBody TransactionRequest request) {

        Transaction transaction = accountService.credit(
                iban,
                request.amount(),
                request.reference());

        return TransactionResponse.from(transaction);
    }

    /**
     * POST /api/accounts/{iban}/debit
     * Bucht Geld von einem Konto ab
     */
    @PostMapping("/{iban}/debit")
    public TransactionResponse debit(
            @PathVariable String iban,
            @Valid @RequestBody TransactionRequest request) {

        Transaction transaction = accountService.debit(
                iban,
                request.amount(),
                request.reference());

        return TransactionResponse.from(transaction);
    }

    /**
     * GET /api/accounts/{iban}/transactions
     * Gibt alle Transaktionen eines Kontos zurück
     */
    @GetMapping("/{iban}/transactions")
    public List<TransactionResponse> getTransactions(@PathVariable String iban) {
        return accountService.getTransactions(iban).stream()
                .map(TransactionResponse::from)
                .toList();
    }
}