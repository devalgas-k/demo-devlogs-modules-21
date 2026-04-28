package com.example.demo.service;

import com.example.demo.domain.Account;
import com.example.demo.domain.Transaction;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankingServiceOptimized {
    
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    
    /**
     * CAS OPTIMISÉ 1: Gestion appropriée des exceptions avec rollback
     * Solution: Utilisation de try-catch et rollback explicite
     */
    @Transactional(rollbackFor = Exception.class)
    public Transaction transferMoneyRobust(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        log.info("Transferring {} from {} to {} (robust version)", amount, fromAccountNumber, toAccountNumber);
        
        Transaction transaction = null;
        try {
            // Verrouiller les comptes dans l'ordre pour éviter les deadlocks
            Account fromAccount = accountRepository.findByAccountNumberForUpdate(
                    fromAccountNumber.compareTo(toAccountNumber) < 0 ? fromAccountNumber : toAccountNumber)
                    .orElseThrow(() -> new RuntimeException("From account not found"));
            
            Account toAccount = accountRepository.findByAccountNumberForUpdate(
                    fromAccountNumber.compareTo(toAccountNumber) < 0 ? toAccountNumber : fromAccountNumber)
                    .orElseThrow(() -> new RuntimeException("To account not found"));
            
            // S'assurer qu'on a les bons comptes
            if (!fromAccount.getAccountNumber().equals(fromAccountNumber)) {
                Account temp = fromAccount;
                fromAccount = toAccount;
                toAccount = temp;
            }
            
            // Vérification du solde avec découverts
            BigDecimal availableBalance = fromAccount.getBalance().add(fromAccount.getOverdraftLimit());
            if (availableBalance.compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient funds including overdraft limit");
            }
            
            // Créer la transaction en premier (pattern Saga)
            transaction = Transaction.builder()
                    .transactionId(UUID.randomUUID().toString())
                    .fromAccount(fromAccount)
                    .toAccount(toAccount)
                    .amount(amount)
                    .status(Transaction.TransactionStatus.PENDING)
                    .build();
            transaction = transactionRepository.save(transaction);
            
            // Effectuer le transfert
            fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
            toAccount.setBalance(toAccount.getBalance().add(amount));
            
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);
            
            // Marquer la transaction comme complétée
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            transactionRepository.save(transaction);
            
            log.info("Transfer completed successfully: {}", transaction.getTransactionId());
            return transaction;
            
        } catch (Exception e) {
            log.error("Transfer failed, rolling back: {}", e.getMessage());
            if (transaction != null) {
                transaction.setStatus(Transaction.TransactionStatus.FAILED);
                transaction.setErrorMessage(e.getMessage());
                transactionRepository.save(transaction);
            }
            throw e; // Re-lancer pour déclencher le rollback
        }
    }
    
    /**
     * CAS OPTIMISÉ 2: Utilisation de verrous pessimistes pour éviter les race conditions
     * Solution: SELECT FOR UPDATE et ordre de verrouillage cohérent
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Transaction transferMoneyWithProperLocking(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        log.info("Transferring {} from {} to {} (with proper locking)", amount, fromAccountNumber, toAccountNumber);
        
        // Verrouiller les comptes dans un ordre déterministe pour éviter les deadlocks
        String firstAccount = fromAccountNumber.compareTo(toAccountNumber) < 0 ? fromAccountNumber : toAccountNumber;
        String secondAccount = fromAccountNumber.compareTo(toAccountNumber) < 0 ? toAccountNumber : fromAccountNumber;
        
        Account account1 = accountRepository.findByAccountNumberForUpdate(firstAccount)
                .orElseThrow(() -> new RuntimeException("Account not found: " + firstAccount));
        
        Account account2 = accountRepository.findByAccountNumberForUpdate(secondAccount)
                .orElseThrow(() -> new RuntimeException("Account not found: " + secondAccount));
        
        // Déterminer quel compte est le compte source
        Account fromAccount = account1.getAccountNumber().equals(fromAccountNumber) ? account1 : account2;
        Account toAccount = account1.getAccountNumber().equals(fromAccountNumber) ? account2 : account1;
        
        // Vérification du solde
        BigDecimal availableBalance = fromAccount.getBalance().add(fromAccount.getOverdraftLimit());
        if (availableBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds including overdraft limit");
        }
        
        // Effectuer le transfert
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        
        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(amount)
                .status(Transaction.TransactionStatus.COMPLETED)
                .build();
        
        return transactionRepository.save(transaction);
    }
    
    /**
     * CAS OPTIMISÉ 3: Timeout approprié et gestion des transactions longues
     * Solution: Timeout adapté et découpage des opérations
     */
    @Transactional(timeout = 60) // 60 secondes pour les opérations longues
    public Transaction transferMoneyWithProperTimeout(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        log.info("Transferring {} from {} to {} (with proper timeout)", amount, fromAccountNumber, toAccountNumber);
        
        // Étape 1: Validation rapide (sans verrous)
        Account fromAccount = accountRepository.findByAccountNumber(fromAccountNumber)
                .orElseThrow(() -> new RuntimeException("From account not found"));
        
        Account toAccount = accountRepository.findByAccountNumber(toAccountNumber)
                .orElseThrow(() -> new RuntimeException("To account not found"));
        
        // Vérification préliminaire du solde
        BigDecimal availableBalance = fromAccount.getBalance().add(fromAccount.getOverdraftLimit());
        if (availableBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds including overdraft limit");
        }
        
        // Étape 2: Opérations longues en dehors de la transaction
        performExternalValidation(fromAccountNumber, toAccountNumber);
        
        // Étape 3: Re-verrouiller et effectuer le transfert
        return transferMoneyWithProperLocking(fromAccountNumber, toAccountNumber, amount);
    }
    
    /**
     * CAS OPTIMISÉ 4: Gestion des deadlocks avec retry
     * Solution: Retry exponentiel et détection des deadlocks
     */
    @Transactional
    public Transaction transferMoneyWithRetry(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                return attemptTransfer(fromAccountNumber, toAccountNumber, amount);
            } catch (Exception e) {
                if (isDeadlockException(e) && retryCount < maxRetries - 1) {
                    retryCount++;
                    log.warn("Deadlock detected, retrying (attempt {})", retryCount);
                    try {
                        TimeUnit.MILLISECONDS.sleep(100 * retryCount); // Backoff exponentiel
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Transfer interrupted", ie);
                    }
                } else {
                    throw e;
                }
            }
        }
        
        throw new RuntimeException("Max retries exceeded for transfer");
    }
    
    private Transaction attemptTransfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        return transferMoneyWithProperLocking(fromAccountNumber, toAccountNumber, amount);
    }
    
    private boolean isDeadlockException(Exception e) {
        String message = e.getMessage();
        return message != null && (
                message.contains("deadlock") ||
                message.contains("DeadlockLoserDataAccessException") ||
                message.contains("Lock wait timeout")
        );
    }
    
    private void performExternalValidation(String fromAccount, String toAccount) {
        // Simulation d'une validation externe (hors transaction)
        try {
            Thread.sleep(100); // Simulation d'un appel API
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}