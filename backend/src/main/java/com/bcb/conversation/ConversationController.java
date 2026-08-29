package com.bcb.conversation;

import com.bcb.auth.AuthenticatedClient;
import com.bcb.common.ErrorResponse;
import com.bcb.conversation.dto.ConversationResponse;
import com.bcb.conversation.dto.ConversationSummary;
import com.bcb.message.MessageService;
import com.bcb.message.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Conversas", description = "Conversas do cliente autenticado com seus destinatários e as mensagens trocadas em cada uma")
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final AuthenticatedClient authenticatedClient;

    @Operation(summary = "Listar conversas do cliente autenticado",
            description = "Retorna as conversas ordenadas pela última mensagem, com a contagem de mensagens não lidas em cada uma.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de conversas (pode ser vazia)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConversationResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public List<ConversationResponse> listConversations() {
        List<ConversationSummary> summaries = conversationService.listConversations(authenticatedClient.getClientId());
        if (summaries.isEmpty()) {
            return List.of();
        }

        // unreadCount cruza Conversation + Message; montado aqui (não em ConversationService) pra
        // não criar dependência circular entre os services (MessageService já depende de
        // ConversationService pro fluxo de envio).
        Map<UUID, Long> unreadCounts = messageService.countUnreadGroupedByConversation(
                summaries.stream().map(ConversationSummary::id).toList());

        return summaries.stream()
                .map(summary -> new ConversationResponse(summary.id(), summary.recipientId(),
                        summary.recipientName(), summary.lastMessageAt(),
                        unreadCounts.getOrDefault(summary.id(), 0L)))
                .toList();
    }

    @Operation(summary = "Listar mensagens de uma conversa",
            description = "Retorna as mensagens da conversa em ordem de envio. A conversa precisa pertencer ao cliente autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de mensagens (pode ser vazia)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MessageResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conversa não encontrada ou não pertence ao cliente autenticado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/messages")
    public List<MessageResponse> listMessages(@Parameter(description = "Id da conversa") @PathVariable UUID id) {
        UUID clientId = authenticatedClient.getClientId();
        conversationService.assertOwnership(clientId, id);
        return messageService.listByConversation(id);
    }
}
