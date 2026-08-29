package com.bcb.transaction.dto;

import com.bcb.domain.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        @Schema(description = "Id da mensagem que originou o débito — null para créditos e ajustes não vinculados a uma mensagem")
        UUID messageId,
        TransactionType type,
        BigDecimal amount,
        LocalDateTime timestamp) {
}
