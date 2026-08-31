package com.example.demo.beanslifecycle.domain;

import com.example.demo.sharedconfig.ExternalIdConfig.ImportedIdGenerator;
import org.springframework.stereotype.Service;

@Service
public class ImportedIdService {

    private final ImportedIdGenerator generator;

    public ImportedIdService(ImportedIdGenerator generator) {
        this.generator = generator;
    }

    public String newId() {
        return generator.newId();
    }
}

