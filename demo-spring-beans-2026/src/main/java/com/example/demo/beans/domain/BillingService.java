package com.example.demo.beans.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {
    private final WidgetRepository repository;

    public BillingService(WidgetRepository repository) {
        this.repository = repository;
    }

    public void outer() {
        inner();
    }

    @Transactional
    public void inner() {
        repository.save(new WidgetRepository.Widget("test"));
        throw new RuntimeException("Rollback expected if transactional");
    }
}
