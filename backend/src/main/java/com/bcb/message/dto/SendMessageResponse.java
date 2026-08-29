package com.bcb.message.dto;

import com.bcb.domain.MessageStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SendMessageResponse(
        UUID id,
        MessageStatus status,
        @Schema(description = "Data/hora em que a mensagem foi enfileirada")
        LocalDateTime timestamp,
        @Schema(description = "Estimativa de entrega — timestamp + tempo fixo de processamento")
        LocalDateTime estimatedDelivery,
        @Schema(description = "Valor cobrado pela prioridade escolhida")
        BigDecimal cost,
        @Schema(description = "Saldo restante após a cobrança — só populado para clientes PREPAID")
        BigDecimal currentBalance) {
}
