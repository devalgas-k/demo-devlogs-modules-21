package com.example.demo.crash.startup;

public class InvalidConfigurationException extends RuntimeException {
    
    private final String detailMessage;
    private final String action;

    public InvalidConfigurationException(String message, String detailMessage, String action) {
        super(message);
        this.detailMessage = detailMessage;
        this.action = action;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public String getAction() {
        return action;
    }
}
