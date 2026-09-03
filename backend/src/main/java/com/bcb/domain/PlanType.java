package com.bcb.domain;

import lombok.Getter;

public enum PlanType {
    PREPAID("pré-pago"),
    POSTPAID("pós-pago");

    @Getter
    final String description;
    PlanType(String description) {
        this.description = description;
    }

    public boolean isPrePaid() {
        return this.equals(PREPAID);
    }
}
