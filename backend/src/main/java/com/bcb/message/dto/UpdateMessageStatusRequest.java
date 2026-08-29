package com.bcb.message.dto;

import com.bcb.domain.MessageStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateMessageStatusRequest(
        @NotNull(message = "Status é obrigatório")
        @Schema(description = "Só DELIVERED ou READ são aceitos aqui — os demais status são geridos pela fila de processamento.")
        MessageStatus status) {
}
