package com.bcb.message;

import com.bcb.client.ClientService;
import com.bcb.client.DocumentId;
import com.bcb.client.dto.ClientResponse;
import com.bcb.conversation.Conversation;
import com.bcb.conversation.ConversationService;
import com.bcb.domain.DocumentType;
import com.bcb.domain.MessagePriority;
import com.bcb.domain.PlanType;
import com.bcb.message.dto.MessageResponse;
import com.bcb.message.dto.SendMessageRequest;
import com.bcb.message.dto.SendMessageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ClientService clientService;

    @Mock
    private ConversationService conversationService;

    @Mock
    private MessageQueueService messageQueueService;

    @InjectMocks
    private MessageService messageService;

    @Test
    void enviarMensagemCobraResolveConversaPersisteEEnfileira() {
        UUID clientId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        SendMessageRequest request = new SendMessageRequest("recipient-1", "Fulano", "Oi", MessagePriority.NORMAL);
        ClientResponse client = clientResponse();
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);

        when(clientService.chargeForMessage(clientId, MessagePriority.NORMAL)).thenReturn(client);
        when(conversationService.resolveConversation(clientId, "recipient-1", "Fulano")).thenReturn(conversationId);
        when(conversationService.getConversationReference(conversationId)).thenReturn(conversation);
        when(messageRepository.save(any())).thenAnswer(invocation -> {
            Message saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        SendMessageResponse response = messageService.send(clientId, request);

        assertThat(response.cost()).isEqualByComparingTo(MessagePriority.NORMAL.getCost());
        assertThat(response.currentBalance()).isEqualByComparingTo(client.balance());
        verify(conversationService).touchLastMessageAt(any(), any());
        verify(messageQueueService).enqueue(any());
    }

    @Test
    void listaMensagensDeUmaConversaEmOrdemDeEnvio() {
        UUID conversationId = UUID.randomUUID();
        Message message = new Message();
        message.setId(UUID.randomUUID());
        message.setPriority(MessagePriority.NORMAL);
        when(messageRepository.findByConversation_IdOrderByQueuedAtAsc(conversationId)).thenReturn(List.of(message));

        List<MessageResponse> result = messageService.listByConversation(conversationId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(message.getId());
    }

    @Test
    void agrupaContagemDeNaoLidasPorConversa() {
        UUID conversationId = UUID.randomUUID();
        MessageRepository.UnreadCount unreadCount = new MessageRepository.UnreadCount() {
            public UUID getConversationId() {
                return conversationId;
            }

            public long getUnreadCount() {
                return 3;
            }
        };
        when(messageRepository.countUnreadGroupedByConversation(List.of(conversationId))).thenReturn(List.of(unreadCount));

        Map<UUID, Long> result = messageService.countUnreadGroupedByConversation(List.of(conversationId));

        assertThat(result).containsEntry(conversationId, 3L);
    }

    private ClientResponse clientResponse() {
        return new ClientResponse(UUID.randomUUID(), "Cliente Teste",
                new DocumentId("12345678901", DocumentType.CPF), PlanType.PREPAID,
                new BigDecimal("9.75"), null, true);
    }
}
