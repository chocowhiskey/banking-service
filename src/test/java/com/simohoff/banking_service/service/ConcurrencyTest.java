package com.simohoff.banking_service.service;

import com.simohoff.banking_service.domain.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ConcurrencyTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransferService transferService;

    private String sourceIban;
    private String targetIban1;
    private String targetIban2;

    @BeforeEach
    void setUp() {
        // Eindeutige IBANs für jeden Test-Durchlauf
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        sourceIban = "DE_SRC_" + uniqueId;
        targetIban1 = "DE_TGT1_" + uniqueId;
        targetIban2 = "DE_TGT2_" + uniqueId;

        // Konten erstellen
        accountService.createAccount(sourceIban, "Source Account");
        accountService.createAccount(targetIban1, "Target 1");
        accountService.createAccount(targetIban2, "Target 2");

        // Source Account mit 1000 EUR aufladen
        accountService.credit(sourceIban, new BigDecimal("1000"), "Initial");
    }

    @Test
    void shouldHandleConcurrentTransfersCorrectly() throws Exception {
        // Given
        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        // When - 2 Threads starten gleichzeitig
        for (int i = 0; i < numberOfThreads; i++) {
            final String targetIban = (i == 0) ? targetIban1 : targetIban2;

            Future<Boolean> future = executor.submit(() -> {
                try {
                    latch.await();

                    transferService.transfer(
                            sourceIban,
                            targetIban,
                            new BigDecimal("300"),
                            "Concurrent Transfer");
                    return true;
                } catch (Exception e) {
                    System.out.println("Transfer failed: " + e.getMessage());
                    return false;
                }
            });

            futures.add(future);
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Ergebnisse sammeln
        int successfulTransfers = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successfulTransfers++;
            }
        }

        // Then
        assertThat(successfulTransfers).isEqualTo(2);

        Account sourceAccount = accountService.getAccount(sourceIban);
        assertThat(sourceAccount.getBalance()).isEqualByComparingTo("400");

        Account target1 = accountService.getAccount(targetIban1);
        Account target2 = accountService.getAccount(targetIban2);
        assertThat(target1.getBalance()).isEqualByComparingTo("300");
        assertThat(target2.getBalance()).isEqualByComparingTo("300");
    }

    @Test
    void shouldHandleMultipleConcurrentTransfers() throws Exception {
        // Given
        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        // When - 10 Threads, jeder überweist 50 EUR
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNum = i;

            Future<Boolean> future = executor.submit(() -> {
                try {
                    latch.await();

                    // Eindeutige IBAN für diesen Thread
                    String targetIban = sourceIban.replace("SRC", "MULTI_" + threadNum);
                    accountService.createAccount(targetIban, "Target " + threadNum);

                    transferService.transfer(
                            sourceIban,
                            targetIban,
                            new BigDecimal("50"),
                            "Transfer #" + threadNum);
                    return true;
                } catch (Exception e) {
                    System.out.println("Multi transfer failed: " + e.getMessage());
                    return false;
                }
            });

            futures.add(future);
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);

        // Then
        int successfulTransfers = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successfulTransfers++;
            }
        }

        assertThat(successfulTransfers).isEqualTo(10);

        Account sourceAccount = accountService.getAccount(sourceIban);
        assertThat(sourceAccount.getBalance()).isEqualByComparingTo("500");
    }

    @Test
    void shouldPreventOverdraftWithConcurrentTransfers() throws Exception {
        // Given
        int numberOfThreads = 3;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        // When - 3 Threads versuchen jeweils 400 EUR abzubuchen
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNum = i;

            Future<Boolean> future = executor.submit(() -> {
                try {
                    latch.await();

                    String targetIban = sourceIban.replace("SRC", "OVER_" + threadNum);
                    accountService.createAccount(targetIban, "Overdraft Test " + threadNum);

                    transferService.transfer(
                            sourceIban,
                            targetIban,
                            new BigDecimal("400"),
                            "Big Transfer #" + threadNum);
                    return true;
                } catch (Exception e) {
                    // Insufficient funds - erwartet!
                    return false;
                }
            });

            futures.add(future);
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);

        // Then
        int successfulTransfers = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successfulTransfers++;
            }
        }

        assertThat(successfulTransfers).isLessThanOrEqualTo(2);

        Account sourceAccount = accountService.getAccount(sourceIban);
        assertThat(sourceAccount.getBalance()).isGreaterThanOrEqualTo(new BigDecimal("200"));
    }
}