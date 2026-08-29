package com.bcb.message;

import com.bcb.domain.MessagePriority;

import java.time.LocalDateTime;
import java.util.UUID;

public record QueuedMessage(UUID messageId, MessagePriority priority, LocalDateTime queuedAt, int attempts) {

    public QueuedMessage nextAttempt() {
        return new QueuedMessage(messageId, priority, queuedAt, attempts + 1);
    }
}
