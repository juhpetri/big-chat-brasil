package com.bcb.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint simples para validar que o backend está de pé e acessível pelo frontend.
 */
@Tag(name = "Ping", description = "Healthcheck do backend")
@RestController
public class PingController {

    @Operation(summary = "Healthcheck", description = "Confirma que o backend está de pé e acessível. Não requer autenticação.")
    @SecurityRequirements
    @GetMapping("/api/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "status", "ok",
                "timestamp", Instant.now().toString()
        );
    }
}
