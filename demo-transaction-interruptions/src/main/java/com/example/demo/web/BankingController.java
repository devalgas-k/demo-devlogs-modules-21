package com.example.demo.web;

import com.example.demo.domain.Transaction;
import com.example.demo.service.BankingServiceNaive;
import com.example.demo.service.BankingServiceOptimized;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/banking")
@RequiredArgsConstructor
@Slf4j
public class BankingController {
    
    private final BankingServiceNaive bankingServiceNaive;
    private final BankingServiceOptimized bankingServiceOptimized;
    
    @PostMapping("/transfer/naive")
    public ResponseEntity<?> transferNaive(@RequestBody TransferRequest request) {
        try {
            Transaction transaction = bankingServiceNaive.transferMoneyNaive(
                    request.getFromAccount(), 
                    request.getToAccount(), 
                    request.getAmount()
            );
            return ResponseEntity.ok(Map.of(
                    "transactionId", transaction.getTransactionId(),
                    "status", transaction.getStatus(),
                    "message", "Transfer completed successfully"
            ));
        } catch (Exception e) {
            log.error("Transfer failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "scenario", "PROBLEM: Money could be lost if exception occurs after debit!"
            ));
        }
    }
    
    @PostMapping("/transfer/race-condition")
    public ResponseEntity<?> transferRaceCondition(@RequestBody TransferRequest request) {
        try {
            Transaction transaction = bankingServiceNaive.transferMoneyWithRaceCondition(
                    request.getFromAccount(), 
                    request.getToAccount(), 
                    request.getAmount()
            );
            return ResponseEntity.ok(Map.of(
                    "transactionId", transaction.getTransactionId(),
                    "status", transaction.getStatus(),
                    "message", "Transfer completed"
            ));
        } catch (Exception e) {
            log.error("Transfer failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "scenario", "PROBLEM: Race conditions possible without proper locking!"
            ));
        }
    }
    
    @PostMapping("/transfer/short-timeout")
    public ResponseEntity<?> transferShortTimeout(@RequestBody TransferRequest request) {
        try {
            Transaction transaction = bankingServiceNaive.transferMoneyWithShortTimeout(
                    request.getFromAccount(), 
                    request.getToAccount(), 
                    request.getAmount()
            );
            return ResponseEntity.ok(Map.of(
                    "transactionId", transaction.getTransactionId(),
                    "status", transaction.getStatus(),
                    "message", "Transfer completed"
            ));
        } catch (Exception e) {
            log.error("Transfer failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "scenario", "PROBLEM: Transaction timeout too short for long operations!"
            ));
        }
    }
    
    @PostMapping("/transfer/robust")
    public ResponseEntity<?> transferRobust(@RequestBody TransferRequest request) {
        try {
            Transaction transaction = bankingServiceOptimized.transferMoneyRobust(
                    request.getFromAccount(), 
                    request.getToAccount(), 
                    request.getAmount()
            );
            return ResponseEntity.ok(Map.of(
                    "transactionId", transaction.getTransactionId(),
                    "status", transaction.getStatus(),
                    "message", "Transfer completed successfully with proper error handling"
            ));
        } catch (Exception e) {
            log.error("Transfer failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "scenario", "SOLUTION: Proper exception handling with rollback!"
            ));
        }
    }
    
    @PostMapping("/transfer/proper-locking")
    public ResponseEntity<?> transferProperLocking(@RequestBody TransferRequest request) {
        try {
            Transaction transaction = bankingServiceOptimized.transferMoneyWithProperLocking(
                    request.getFromAccount(), 
                    request.getToAccount(), 
                    request.getAmount()
            );
            return ResponseEntity.ok(Map.of(
                    "transactionId", transaction.getTransactionId(),
                    "status", transaction.getStatus(),
                    "message", "Transfer completed with proper locking"
            ));
        } catch (Exception e) {
            log.error("Transfer failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "scenario", "SOLUTION: Proper locking prevents race conditions!"
            ));
        }
    }
    
    @PostMapping("/transfer/proper-timeout")
    public ResponseEntity<?> transferProperTimeout(@RequestBody TransferRequest request) {
        try {
            Transaction transaction = bankingServiceOptimized.transferMoneyWithProperTimeout(
                    request.getFromAccount(), 
                    request.getToAccount(), 
                    request.getAmount()
            );
            return ResponseEntity.ok(Map.of(
                    "transactionId", transaction.getTransactionId(),
                    "status", transaction.getStatus(),
                    "message", "Transfer completed with proper timeout handling"
            ));
        } catch (Exception e) {
            log.error("Transfer failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "scenario", "SOLUTION: Proper timeout configuration!"
            ));
        }
    }
    
    @PostMapping("/transfer/with-retry")
    public ResponseEntity<?> transferWithRetry(@RequestBody TransferRequest request) {
        try {
            Transaction transaction = bankingServiceOptimized.transferMoneyWithRetry(
                    request.getFromAccount(), 
                    request.getToAccount(), 
                    request.getAmount()
            );
            return ResponseEntity.ok(Map.of(
                    "transactionId", transaction.getTransactionId(),
                    "status", transaction.getStatus(),
                    "message", "Transfer completed with retry mechanism"
            ));
        } catch (Exception e) {
            log.error("Transfer failed after retries: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "scenario", "SOLUTION: Retry mechanism handles deadlocks!"
            ));
        }
    }
    
    @GetMapping("/scenarios")
    public ResponseEntity<?> getScenarios() {
        Map<String, Object> scenarios = new HashMap<>();
        
        scenarios.put("naive", Map.of(
                "description", "Pas de gestion d'exception - argent perdu possible",
                "endpoint", "/api/banking/transfer/naive",
                "problem", "Si exception après débit, argent perdu"
        ));
        
        scenarios.put("raceCondition", Map.of(
                "description", "Race conditions sans verrous appropriés",
                "endpoint", "/api/banking/transfer/race-condition",
                "problem", "Mise à jour concurrente possible"
        ));
        
        scenarios.put("shortTimeout", Map.of(
                "description", "Timeout trop court pour opérations longues",
                "endpoint", "/api/banking/transfer/short-timeout",
                "problem", "Transactions longues échouent inutilement"
        ));
        
        scenarios.put("robust", Map.of(
                "description", "Gestion appropriée des exceptions avec rollback",
                "endpoint", "/api/banking/transfer/robust",
                "solution", "Try-catch et rollback explicite"
        ));
        
        scenarios.put("properLocking", Map.of(
                "description", "Verrous pessimistes pour éviter race conditions",
                "endpoint", "/api/banking/transfer/proper-locking",
                "solution", "SELECT FOR UPDATE et ordre déterministe"
        ));
        
        scenarios.put("properTimeout", Map.of(
                "description", "Configuration appropriée du timeout",
                "endpoint", "/api/banking/transfer/proper-timeout",
                "solution", "Timeout adapté et opérations longues hors transaction"
        ));
        
        scenarios.put("withRetry", Map.of(
                "description", "Mécanisme de retry pour deadlocks",
                "endpoint", "/api/banking/transfer/with-retry",
                "solution", "Retry exponentiel et détection des deadlocks"
        ));
        
        return ResponseEntity.ok(scenarios);
    }
    
    public static class TransferRequest {
        private String fromAccount;
        private String toAccount;
        private BigDecimal amount;
        
        // Getters and setters
        public String getFromAccount() { return fromAccount; }
        public void setFromAccount(String fromAccount) { this.fromAccount = fromAccount; }
        
        public String getToAccount() { return toAccount; }
        public void setToAccount(String toAccount) { this.toAccount = toAccount; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }
}