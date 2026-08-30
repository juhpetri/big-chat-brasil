package com.bcb.domain;

import lombok.Getter;

import java.math.BigDecimal;

public enum MessagePriority {
    NORMAL(BigDecimal.valueOf(0.25), "normal"),
    URGENT(BigDecimal.valueOf(0.50), "urgente");

    @Getter
    final BigDecimal cost;
    @Getter
    final String priorityLabel;

    MessagePriority(BigDecimal cost, String priorityLabel) {
        this.cost = cost;
        this.priorityLabel = priorityLabel;
    }

    public int priority() {
        return this == URGENT ? 0 : 1;
    }
}
