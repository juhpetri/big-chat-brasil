package com.bcb.client;

import com.bcb.client.dto.ClientResponse;
import com.bcb.client.exceptions.ClientInactiveException;
import com.bcb.client.exceptions.InvalidPlanOperationException;
import com.bcb.domain.DocumentType;
import com.bcb.domain.PlanType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static java.util.Objects.nonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "client")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(columnDefinition = "TEXT")
    private String name;

    @Embedded
    private DocumentId documentId;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "TEXT")
    private PlanType planType;

    private Boolean active;

    @Column(precision = 10, scale = 2)
    private BigDecimal balance;

    @Column(precision = 10, scale = 2)
    private BigDecimal monthlyLimit;

    @Column(precision = 10, scale = 2)
    private BigDecimal monthlyUsage;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public ClientResponse toClientResponse() {
        BigDecimal remainingLimit = nonNull(monthlyLimit) ? monthlyLimit.subtract(monthlyUsage) : BigDecimal.ZERO;
        return new ClientResponse(id, name, documentId,
                planType, balance, remainingLimit, active);
    }

    public BigDecimal getResidualValue() {
        return PlanType.PREPAID.equals(planType)
                ? balance
                : monthlyLimit.subtract(monthlyUsage);
    }

    public void checkAlreadyPlanType(PlanType newPlanType) {
        if (planType.equals(newPlanType)) {
            throw new InvalidPlanOperationException("Cliente já está no plano " + newPlanType + ".");
        }
    }

    public void checkIsPrepaid() {
        if (planType != PlanType.PREPAID) {
            throw new InvalidPlanOperationException("Só é possível adicionar crédito a clientes PREPAID.");
        }
    }

    public void checkIsPostpaid() {
        if (planType != PlanType.POSTPAID) {
            throw new InvalidPlanOperationException("Só é possível ajustar limite de clientes POSTPAID.");
        }
    }

    public void convertToPlanType(BigDecimal initialValue, PlanType newPlanType) {
        planType = newPlanType;
        if (PlanType.PREPAID.equals(planType)) {
            convertToPrePaid(initialValue);
            return;
        }

        convertToPosPaid(initialValue);
    }

    private void convertToPrePaid(BigDecimal initialValue) {
        balance = initialValue;
        monthlyLimit = null;
        monthlyUsage = null;
    }

    private void convertToPosPaid(BigDecimal initialValue) {
        monthlyLimit = initialValue;
        monthlyUsage = BigDecimal.ZERO;
        balance = null;
    }
}
