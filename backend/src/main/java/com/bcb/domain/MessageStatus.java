package com.bcb.domain;

import com.bcb.message.exceptions.InvalidMessageStatusTargetException;
import com.bcb.message.exceptions.InvalidMessageStatusTransitionException;

import java.util.Set;

public enum MessageStatus {
    QUEUED(Set.of()),
    PROCESSING(Set.of()),
    SENT(Set.of()),
    DELIVERED(Set.of(SENT)),
    READ(Set.of(SENT, DELIVERED)),
    FAILED(Set.of());

    private final Set<MessageStatus> allowedSourceStatuses;

    MessageStatus(Set<MessageStatus> allowedSourceStatuses) {
        this.allowedSourceStatuses = allowedSourceStatuses;
    }

    public void checkManuallySettable() {
        if (allowedSourceStatuses.isEmpty()) {
            throw new InvalidMessageStatusTargetException(this);
        }
    }

    public void checkTransitionFrom(MessageStatus current) {
        if (!allowedSourceStatuses.contains(current)) {
            throw new InvalidMessageStatusTransitionException(current, this);
        }
    }
}
