package com.example.demo.crash.runtime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiController {

    @GetMapping("/api/data")
    public String getData(@RequestParam(required = false) String param) {
        if ("fail".equals(param)) {
            throw new IllegalArgumentException("Paramètre 'fail' non autorisé au runtime !");
        }
        return "Succès de l'API";
    }
}
