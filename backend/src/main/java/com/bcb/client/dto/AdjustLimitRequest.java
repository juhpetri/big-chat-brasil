package com.bcb.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdjustLimitRequest(
        @NotNull(message = "Novo limite é obrigatório")
        @DecimalMin(value = "0", message = "Limite não pode ser negativo")
        @Schema(description = "Novo limite mensal — só válido pra clientes POSTPAID; o consumo do mês (monthlyUsage) não é resetado")
        BigDecimal newLimit) {
}
