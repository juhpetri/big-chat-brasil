package com.bcb.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank(message = "Número do documento é obrigatório")
        @Schema(description = "CPF (11 dígitos) ou CNPJ (14 dígitos) do cliente já cadastrado, com ou sem máscara", example = "12345678901")
        String document) {
}
