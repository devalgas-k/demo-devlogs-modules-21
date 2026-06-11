package com.example.demo.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiController {

    @GetMapping("/api/public")
    public Map<String, Object> publicEndpoint() {
        return Map.of(
                "status", "ok",
                "message", "public"
        );
    }

    @GetMapping("/api/secure")
    @PreAuthorize("hasAuthority('SCOPE_read')")
    public Map<String, Object> secure(Authentication authentication) {
        return Map.of(
                "status", "ok",
                "principal", authentication.getName(),
                "authorities", authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList()
        );
    }

    @GetMapping("/api/admin")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public Map<String, Object> admin(Authentication authentication) {
        return Map.of(
                "status", "ok",
                "principal", authentication.getName(),
                "message", "admin"
        );
    }
}
