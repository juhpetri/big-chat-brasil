package com.bcb.client.dto;

import com.bcb.client.Client;
import com.bcb.client.DocumentId;
import com.bcb.domain.DocumentType;
import com.bcb.domain.PlanType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateClientRequest(
        @NotBlank(message = "Nome deve ser informado")
        String name,
        @NotBlank(message = "Número do documento deve ser informado")
        @Schema(description = "CPF (11 dígitos) ou CNPJ (14 dígitos), com ou sem máscara — o tipo é inferido pelo tamanho, não é um campo separado", example = "12345678901")
        String document,
        @NotNull(message = "Tipo de plano não informado")
        @Schema(description = "PREPAID usa balance (saldo debitado por mensagem); POSTPAID usa initialLimit (limite mensal)")
        PlanType planType,
        @Schema(description = "Saldo inicial — só usado se planType = PREPAID")
        BigDecimal balance,
        @DecimalMin(message = "Valor inicial deve ser maior ou igual a \"0\"", value = "0")
        @Schema(description = "Limite mensal — só usado se planType = POSTPAID")
        BigDecimal initialLimit) {

    public Client toClient() {
        DocumentId documentId = DocumentId.of(document);
        Client.ClientBuilder clientBuilder = Client.builder()
                .active(true)
                .documentId(documentId)
                .name(name)
                .planType(planType);


        if (PlanType.PREPAID.equals(planType)) {
            clientBuilder.balance(balance);
        } else {
            clientBuilder.monthlyLimit(initialLimit);
            clientBuilder.monthlyUsage(BigDecimal.ZERO);
        }

        return clientBuilder.build();
    }
}
