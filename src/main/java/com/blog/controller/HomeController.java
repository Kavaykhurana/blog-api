package com.blog.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, String> welcome() {
        return Map.of(
            "status", "UP",
            "name", "Blog API",
            "version", "1.0.0",
            "message", "Welcome to the Blog API. Use POST /api/v1/auth/login to authenticate."
        );
    }
}
