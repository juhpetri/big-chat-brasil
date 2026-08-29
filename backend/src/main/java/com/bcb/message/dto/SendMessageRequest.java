package com.bcb.message.dto;

import com.bcb.domain.MessagePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendMessageRequest(
        @NotBlank(message = "Destinatário é obrigatório")
        @Schema(description = "ID do cliente final que recebe a mensagem — resolve ou cria a conversa junto com recipientName")
        String recipientId,
        @Schema(description = "Nome do destinatário, usado só se a conversa ainda não existir")
        String recipientName,
        @NotBlank(message = "Conteúdo da mensagem é obrigatório")
        String content,
        @NotNull(message = "Prioridade é obrigatória")
        @Schema(description = "NORMAL custa R$0,25 e segue FIFO; URGENT custa R$0,50 e fura a fila de mensagens ainda não processadas")
        MessagePriority priority) {
}
