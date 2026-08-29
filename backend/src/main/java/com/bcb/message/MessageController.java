package com.bcb.message;

import com.bcb.auth.AuthenticatedClient;
import com.bcb.common.ErrorResponse;
import com.bcb.message.dto.MessageResponse;
import com.bcb.message.dto.SendMessageRequest;
import com.bcb.message.dto.SendMessageResponse;
import com.bcb.message.dto.UpdateMessageStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Mensagens", description = "Envio de mensagens do cliente autenticado para um destinatário, com cobrança e fila de prioridade")
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final AuthenticatedClient authenticatedClient;

    @Operation(summary = "Enviar mensagem",
            description = "Cobra o cliente autenticado pela prioridade escolhida, resolve (ou cria) a conversa com o "
                    + "destinatário e enfileira a mensagem para processamento.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Mensagem enfileirada",
                    content = @Content(schema = @Schema(implementation = SendMessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload inválido ou limite mensal excedido (plano POSTPAID)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "402", description = "Saldo insuficiente (plano PREPAID)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente autenticado não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SendMessageResponse sendMessage(@Valid @RequestBody SendMessageRequest request) {
        return messageService.send(authenticatedClient.getClientId(), request);
    }

    @Operation(summary = "Atualizar status de uma mensagem",
            description = "Marca uma mensagem como DELIVERED (a partir de SENT) ou READ (a partir de SENT ou "
                    + "DELIVERED). Os demais status (QUEUED, PROCESSING, SENT, FAILED) são geridos só pela fila "
                    + "de processamento e não podem ser setados por aqui.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status atualizado",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Status alvo inválido ou transição não permitida a partir do status atual",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Mensagem não encontrada ou não pertence ao cliente autenticado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/status")
    public MessageResponse updateStatus(@Parameter(description = "Id da mensagem") @PathVariable UUID id,
                                         @Valid @RequestBody UpdateMessageStatusRequest request) {
        return messageService.updateStatus(authenticatedClient.getClientId(), id, request.status());
    }
}
