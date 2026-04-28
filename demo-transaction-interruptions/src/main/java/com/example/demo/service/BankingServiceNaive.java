package com.example.demo.service;

import com.example.demo.domain.Account;
import com.example.demo.domain.Transaction;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankingServiceNaive {
    
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    
    /**
     * CAS NON-OPTIMISÉ 1: Pas de gestion d'exception appropriée
     * Problème: Si une exception survient après le débit, l'argent est perdu
     */
    @Transactional
    public Transaction transferMoneyNaive(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        log.info("Transferring {} from {} to {}", amount, fromAccountNumber, toAccountNumber);
        
        Account fromAccount = accountRepository.findByAccountNumber(fromAccountNumber)
                .orElseThrow(() -> new RuntimeException("From account not found"));
        
        Account toAccount = accountRepository.findByAccountNumber(toAccountNumber)
                .orElseThrow(() -> new RuntimeException("To account not found"));
        
        // Vérification du solde
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        
        // Débit du compte source
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        accountRepository.save(fromAccount);
        
        // CRITIQUE: Pas de gestion d'exception - si une erreur survient ici, l'argent est perdu!
        // Simulation d'une erreur réseau ou base de données
        if (Math.random() < 0.1) { // 10% de chance d'échec
            throw new RuntimeException("Network error during credit operation");
        }
        
        // Crédit du compte destination
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(toAccount);
        
        // Création de la transaction
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
     * CAS NON-OPTIMISÉ 2: Mauvaise gestion des verrous
     * Problème: Race conditions possibles
     */
    @Transactional
    public Transaction transferMoneyWithRaceCondition(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        log.info("Transferring {} from {} to {} (with race condition risk)", amount, fromAccountNumber, toAccountNumber);
        
        // PROBLÈME: Pas de verrou pessimiste, lecture sans verrou
        Account fromAccount = accountRepository.findByAccountNumber(fromAccountNumber)
                .orElseThrow(() -> new RuntimeException("From account not found"));
        
        Account toAccount = accountRepository.findByAccountNumber(toAccountNumber)
                .orElseThrow(() -> new RuntimeException("To account not found"));
        
        // Vérification du solde
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        
        // Simulation d'un délai pour exposer la race condition
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Les opérations de mise à jour peuvent écraser des changements concurrents
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
     * CAS NON-OPTIMISÉ 3: Timeout de transaction trop court
     * Problème: Les transactions longues échouent inutilement
     */
    @Transactional(timeout = 5) // 5 secondes seulement!
    public Transaction transferMoneyWithShortTimeout(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        log.info("Transferring {} from {} to {} (with short timeout)", amount, fromAccountNumber, toAccountNumber);
        
        Account fromAccount = accountRepository.findByAccountNumber(fromAccountNumber)
                .orElseThrow(() -> new RuntimeException("From account not found"));
        
        Account toAccount = accountRepository.findByAccountNumber(toAccountNumber)
                .orElseThrow(() -> new RuntimeException("To account not found"));
        
        // Simulation d'une opération longue (appel externe, traitement complexe)
        try {
            Thread.sleep(6000); // 6 secondes > timeout de 5 secondes
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        
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
}