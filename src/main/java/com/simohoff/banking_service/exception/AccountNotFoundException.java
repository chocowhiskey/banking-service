package com.simohoff.banking_service.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String iban) {
        super("Account not found with IBAN: " + iban);
    }
}