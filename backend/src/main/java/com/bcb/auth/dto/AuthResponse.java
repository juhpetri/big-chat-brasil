package com.bcb.auth.dto;

import com.bcb.client.dto.ClientResponse;
import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
        @Schema(description = "Token de sessão — enviar como 'Authorization: Bearer <token>' nos demais endpoints")
        String token,
        ClientResponse client) {
}
