package com.bcb.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        String recipientId,
        String recipientName,
        LocalDateTime lastMessageAt,
        @Schema(description = "Quantidade de mensagens do destinatário ainda não lidas pelo cliente")
        long unreadCount) {
}
