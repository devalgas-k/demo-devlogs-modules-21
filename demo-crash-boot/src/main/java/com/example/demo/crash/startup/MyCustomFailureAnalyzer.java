package com.example.demo.crash.startup;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class MyCustomFailureAnalyzer extends AbstractFailureAnalyzer<InvalidConfigurationException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, InvalidConfigurationException cause) {
        String description = "Démarrage échoué à cause d'une configuration invalide : \n" + cause.getDetailMessage();
        String action = "Action requise : " + cause.getAction();
        
        return new FailureAnalysis(description, action, cause);
    }
}
