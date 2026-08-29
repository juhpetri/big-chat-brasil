package com.bcb.client.dto;

import com.bcb.domain.PlanType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ConvertPlanRequest(
        @NotNull(message = "Novo tipo de plano é obrigatório")
        PlanType newPlanType,
        @NotNull(message = "Valor inicial do novo plano é obrigatório")
        @DecimalMin(value = "0", message = "Valor inicial não pode ser negativo")
        @Schema(description = "Saldo inicial (se newPlanType = PREPAID) ou limite mensal inicial (se newPlanType = POSTPAID). "
                + "O valor residual do plano anterior (saldo restante ou limite não usado) é registrado no histórico de transações, não é somado automaticamente aqui.")
        BigDecimal initialValue) {
}
