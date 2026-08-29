package com.bcb.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AddCreditRequest(
        @NotNull(message = "Valor do crédito é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor do crédito deve ser maior que zero")
        @Schema(description = "Valor a somar ao saldo — só válido pra clientes PREPAID")
        BigDecimal amount) {
}
