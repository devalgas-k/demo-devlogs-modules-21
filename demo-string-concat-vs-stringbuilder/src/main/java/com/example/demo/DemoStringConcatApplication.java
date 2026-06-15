package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de la démo "String concat dans une boucle vs StringBuilder".
 * <p>
 * Expose deux endpoints équivalents fonctionnellement, mais très différents
 * en termes d'allocations mémoire et de latence :
 * <ul>
 *   <li>{@code GET /api/concat} — utilise {@code String +=} dans une boucle (anti-pattern)</li>
 *   <li>{@code GET /api/builder} — utilise {@code StringBuilder} (recommandé)</li>
 * </ul>
 * <p>
 * Le but est de mesurer l'écart via Gatling, et de matérialiser ce que coûtent
 * réellement ces allocations en charge concurrente.
 */
@SpringBootApplication
public class DemoStringConcatApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoStringConcatApplication.class, args);
    }
}
