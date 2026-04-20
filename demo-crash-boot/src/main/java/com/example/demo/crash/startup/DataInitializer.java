package com.example.demo.crash.startup;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class DataInitializer implements CommandLineRunner {

    @Value("${app.mode:}")
    private String mode;

    @Override
    public void run(String... args) throws Exception {
        if ("simulate-error".equals(mode)) {
            // Simulation d'une erreur technique au démarrage non gérée par Validation
            throw new InvalidConfigurationException(
                    "Connexion DB impossible",
                    "Les identifiants de la base de données (modele-article) sont invalides ou le port 5432 est bloqué.",
                    "Vérifiez que le serveur PostgreSQL tourne et que les credentials (spring.datasource.*) sont corrects dans application.yml."
            );
        }
    }
}
