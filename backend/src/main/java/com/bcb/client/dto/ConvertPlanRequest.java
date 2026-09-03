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
                + "Ao migrar de PREPAID para POSTPAID, o saldo restante (dinheiro real do cliente) é somado a este valor e "
                + "registrado como um único crédito no histórico de transações. Ao migrar de POSTPAID para PREPAID, o limite "
                + "mensal não usado nunca foi saldo do cliente, então é registrado como débito (zerando-o) e este valor inicial "
                + "é registrado separadamente como crédito — os dois lançamentos ficam visíveis no histórico.")
        BigDecimal initialValue) {
}
