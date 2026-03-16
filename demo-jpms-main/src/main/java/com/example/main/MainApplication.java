package com.example.main;

import com.example.demo.dto.ArticleDTO;
import com.example.demo.dto.ArticleRecord;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MainApplication {

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }

    @Bean
    public CommandLineRunner run() {
        return args -> {
            System.out.println("🚀 JPMS Demo Started!");
            
            // Testing access to classes from the other module
            ArticleDTO dto = new ArticleDTO(1L, "JPMS DTO", "Exploring Java Modules", "Devalgas");
            ArticleRecord record = new ArticleRecord(2L, "JPMS Record", "Learning Java 17+ Features", "Devalgas");

            System.out.println("DTO: " + dto.getTitle());
            System.out.println("Record: " + record.title());
            
            System.out.println("✅ JPMS Module Access Successful!");
        };
    }
}
