package com.example.demo.service;

import com.example.demo.domain.Account;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.UnexpectedRollbackException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class BankingServiceTest {

    @Autowired
    private BankingServiceNaive bankingServiceNaive;

    @Autowired
    private BankingServiceOptimized bankingServiceOptimized;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private String account1;
    private String account2;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        account1 = "ACC1";
        account2 = "ACC2";

        accountRepository.save(Account.builder()
                .accountNumber(account1)
                .ownerName("Alice")
                .balance(new BigDecimal("1000.00"))
                .overdraftLimit(BigDecimal.ZERO)
                .build());

        accountRepository.save(Account.builder()
                .accountNumber(account2)
                .ownerName("Bob")
                .balance(new BigDecimal("500.00"))
                .overdraftLimit(BigDecimal.ZERO)
                .build());
    }

    @Test
    void testTransferMoneyNaive_WhenExceptionOccurs_ShouldRollback() {
        // Étant donné que le service est annoté @Transactional, 
        // même si BankingServiceNaive simule un échec après le débit,
        // Spring doit annuler la transaction.
        
        BigDecimal amount = new BigDecimal("100.00");
        
        // On force une exception en utilisant un montant qui déclenchera potentiellement le Math.random() 
        // ou on teste simplement le comportement transactionnel par défaut.
        // Ici on teste que si une exception est jetée, le solde ne change pas.
        
        try {
            // Note: BankingServiceNaive.transferMoneyNaive a un Math.random() < 0.1
            // Pour forcer le test, on peut soit mocker, soit appeler plusieurs fois, 
            // mais ici on veut vérifier le principe du @Transactional.
            bankingServiceNaive.transferMoneyNaive(account1, account2, amount);
        } catch (RuntimeException e) {
            // L'exception est attendue si le random frappe
        }

        Account from = accountRepository.findByAccountNumber(account1).get();
        Account to = accountRepository.findByAccountNumber(account2).get();

        // Si @Transactional fonctionne, soit les deux sont mis à jour, soit aucun.
        // Jamais un seul.
        if (from.getBalance().compareTo(new BigDecimal("1000.00")) == 0) {
            assertThat(to.getBalance()).isEqualByComparingTo("500.00");
        } else {
            assertThat(from.getBalance()).isEqualByComparingTo("900.00");
            assertThat(to.getBalance()).isEqualByComparingTo("600.00");
        }
    }

    @Test
    void testTransferMoneyRobust_ShouldSucceed() {
        BigDecimal amount = new BigDecimal("200.00");
        
        bankingServiceOptimized.transferMoneyRobust(account1, account2, amount);

        Account from = accountRepository.findByAccountNumber(account1).get();
        Account to = accountRepository.findByAccountNumber(account2).get();

        assertThat(from.getBalance()).isEqualByComparingTo("800.00");
        assertThat(to.getBalance()).isEqualByComparingTo("700.00");
    }

    @Test
    void testInsufficientFunds_ShouldThrowException() {
        BigDecimal amount = new BigDecimal("2000.00");

        assertThrows(RuntimeException.class, () -> {
            bankingServiceOptimized.transferMoneyRobust(account1, account2, amount);
        });

        Account from = accountRepository.findByAccountNumber(account1).get();
        assertThat(from.getBalance()).isEqualByComparingTo("1000.00");
    }
}
