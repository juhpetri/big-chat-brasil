package com.bcb.client.dto;

import com.bcb.client.Client;
import com.bcb.client.DocumentId;
import com.bcb.client.exceptions.ClientInactiveException;
import com.bcb.domain.DocumentType;
import com.bcb.domain.PlanType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record ClientResponse(UUID id,
                             String name,
                             DocumentId documentId,
                             PlanType planType,
                             @Schema(description = "Saldo atual — só populado para clientes PREPAID")
                             BigDecimal balance,
                             @Schema(description = "Limite mensal — só populado para clientes POSTPAID")
                             BigDecimal limit,
                             @Schema(description = "Se false, o cliente não consegue autenticar nem enviar mensagens")
                             boolean active) {

    public void assertActive() {
        if (!active) {
            throw new ClientInactiveException(name);
        }
    }

    public Client toClient() {
        return Client.builder()
                .id(id)
                .name(name)
                .documentId(documentId)
                .planType(planType)
                .balance(balance)
                .monthlyLimit(limit)
                .active(active)
                .build();
    }
}
