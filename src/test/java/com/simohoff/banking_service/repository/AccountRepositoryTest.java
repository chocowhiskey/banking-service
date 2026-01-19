package com.simohoff.banking_service.repository;

import com.simohoff.banking_service.domain.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void shouldSaveAndFindAccount() {
        // Given
        Account account = new Account("DE89370400440532013000", "Max Mustermann");

        // When
        Account saved = accountRepository.save(account);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getIban()).isEqualTo("DE89370400440532013000");
        assertThat(saved.getOwnerName()).isEqualTo("Max Mustermann");
    }

    @Test
    void shouldFindAccountByIban() {
        // Given
        Account account = new Account("DE89370400440532013000", "Max Mustermann");
        accountRepository.save(account);

        // When
        Optional<Account> found = accountRepository.findByIban("DE89370400440532013000");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getOwnerName()).isEqualTo("Max Mustermann");
    }

    @Test
    void shouldReturnEmptyWhenIbanNotFound() {
        // When
        Optional<Account> found = accountRepository.findByIban("NOTEXISTING");

        // Then
        assertThat(found).isEmpty();
    }
}