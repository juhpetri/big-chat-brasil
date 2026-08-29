package com.bcb.domain;

import lombok.Getter;

import java.math.BigDecimal;

public enum MessagePriority {
    NORMAL(BigDecimal.valueOf(0.25)),
    URGENT(BigDecimal.valueOf(0.50));

    @Getter
    final BigDecimal cost;

    MessagePriority(BigDecimal cost) {
        this.cost = cost;
    }

    public int priority() {
        return this == URGENT ? 0 : 1;
    }
}
