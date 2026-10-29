package br.puc.cofre.backend2.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/status")
public class StatusController {

    @GetMapping
    public Map<String, String> verificarStatus() {
        return Map.of(
            "status", "ok",
            "servico", "Backend 2 - Criptografia"
        );
    }
}