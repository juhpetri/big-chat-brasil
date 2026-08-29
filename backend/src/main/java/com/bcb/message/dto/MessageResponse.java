package com.bcb.message.dto;

import com.bcb.domain.MessagePriority;
import com.bcb.domain.MessageStatus;
import com.bcb.domain.SenderType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        String content,
        MessagePriority priority,
        MessageStatus status,
        BigDecimal cost,
        SenderType sentByType,
        LocalDateTime queuedAt,
        @Schema(description = "Preenchido quando a fila processa a mensagem — null enquanto status = QUEUED")
        LocalDateTime processedAt) {
}
