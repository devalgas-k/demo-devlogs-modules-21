package com.example.demo.crash.startup;

import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class StartupFailureListener implements ApplicationListener<ApplicationFailedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(StartupFailureListener.class);

    @Override
    public void onApplicationEvent(ApplicationFailedEvent event) {
        // En cas d'échec au démarrage, on peut envoyer une alerte, purger des fichiers temporaires...
        logger.error("🚨 CRASH AU BOOT DÉTECTÉ 🚨");
        logger.error("Alerte DevOps : L'application n'a pas pu démarrer.");
        logger.error("Cause principale : {}", event.getException().getMessage());
        
        // Exemple d'action : webhookSlack.send("App Crash on startup...");
    }
}
