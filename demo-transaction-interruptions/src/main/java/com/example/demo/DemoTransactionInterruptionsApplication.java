package com.example.demo;

import com.example.demo.domain.Account;
import com.example.demo.repository.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@SpringBootApplication
public class DemoTransactionInterruptionsApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoTransactionInterruptionsApplication.class, args);
	}

	@Bean
	public CommandLineRunner initData(AccountRepository accountRepository) {
		return args -> {
			// Créer des comptes de test
			if (accountRepository.count() == 0) {
				accountRepository.save(Account.builder()
						.accountNumber("ACC001")
						.ownerName("John Doe")
						.balance(new BigDecimal("1000.00"))
						.overdraftLimit(new BigDecimal("500.00"))
						.build());
				
				accountRepository.save(Account.builder()
						.accountNumber("ACC002")
						.ownerName("Jane Smith")
						.balance(new BigDecimal("2000.00"))
						.overdraftLimit(new BigDecimal("1000.00"))
						.build());
				
				accountRepository.save(Account.builder()
						.accountNumber("ACC003")
						.ownerName("Bob Johnson")
						.balance(new BigDecimal("500.00"))
						.overdraftLimit(new BigDecimal("200.00"))
						.build());
				
				System.out.println("✅ Comptes de test créés avec succès!");
				System.out.println("📋 ACC001 - Balance: 1000.00€");
				System.out.println("📋 ACC002 - Balance: 2000.00€");
				System.out.println("📋 ACC003 - Balance: 500.00€");
				System.out.println("");
				System.out.println("🚀 Application démarrée!");
				System.out.println("📖 Documentation API: http://localhost:8080/api/banking/scenarios");
				System.out.println("🧪 Testez les différents scénarios d'interruption de transactions!");
			}
		};
	}
}