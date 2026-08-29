package com.bcb.conversation.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationSummary(
        UUID id,
        String recipientId,
        String recipientName,
        LocalDateTime lastMessageAt) {
}
