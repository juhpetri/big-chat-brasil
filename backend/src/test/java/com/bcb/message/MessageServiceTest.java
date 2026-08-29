package com.bcb.message;

import com.bcb.client.ClientService;
import com.bcb.client.DocumentId;
import com.bcb.client.dto.ClientResponse;
import com.bcb.conversation.Conversation;
import com.bcb.conversation.ConversationService;
import com.bcb.domain.DocumentType;
import com.bcb.domain.MessagePriority;
import com.bcb.domain.MessageStatus;
import com.bcb.domain.PlanType;
import com.bcb.message.dto.MessageResponse;
import com.bcb.message.dto.SendMessageRequest;
import com.bcb.message.dto.SendMessageResponse;
import com.bcb.message.exceptions.InvalidMessageStatusTargetException;
import com.bcb.message.exceptions.InvalidMessageStatusTransitionException;
import com.bcb.message.exceptions.MessageNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

        assertThat(response.conversationId()).isEqualTo(conversationId);
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

    @Test
    void marcaMensagemSentComoDelivered() {
        UUID clientId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Message message = messageWithStatus(messageId, conversationId, MessageStatus.SENT);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageRepository.save(message)).thenReturn(message);

        MessageResponse response = messageService.updateStatus(clientId, messageId, MessageStatus.DELIVERED);

        assertThat(response.status()).isEqualTo(MessageStatus.DELIVERED);
        verify(conversationService).assertOwnership(clientId, conversationId);
    }

    @Test
    void marcaMensagemDeliveredComoRead() {
        UUID clientId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Message message = messageWithStatus(messageId, conversationId, MessageStatus.DELIVERED);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageRepository.save(message)).thenReturn(message);

        MessageResponse response = messageService.updateStatus(clientId, messageId, MessageStatus.READ);

        assertThat(response.status()).isEqualTo(MessageStatus.READ);
    }

    @Test
    void marcarComoDeliveredUmaMensagemQueuedLancaExcecao() {
        UUID clientId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Message message = messageWithStatus(messageId, UUID.randomUUID(), MessageStatus.QUEUED);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> messageService.updateStatus(clientId, messageId, MessageStatus.DELIVERED))
                .isInstanceOf(InvalidMessageStatusTransitionException.class);

        verify(messageRepository, never()).save(any());
    }

    @Test
    void marcarComoQueuedLancaExcecaoDeStatusInvalido() {
        UUID clientId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        assertThatThrownBy(() -> messageService.updateStatus(clientId, messageId, MessageStatus.QUEUED))
                .isInstanceOf(InvalidMessageStatusTargetException.class);

        verify(messageRepository, never()).findById(any());
    }

    @Test
    void atualizarStatusDeMensagemInexistenteLancaExcecao() {
        UUID clientId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.updateStatus(clientId, messageId, MessageStatus.DELIVERED))
                .isInstanceOf(MessageNotFoundException.class);
    }

    private Message messageWithStatus(UUID messageId, UUID conversationId, MessageStatus status) {
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);

        Message message = new Message();
        message.setId(messageId);
        message.setConversation(conversation);
        message.setStatus(status);
        return message;
    }

    private ClientResponse clientResponse() {
        return new ClientResponse(UUID.randomUUID(), "Cliente Teste",
                new DocumentId("12345678901", DocumentType.CPF), PlanType.PREPAID,
                new BigDecimal("9.75"), null, true);
    }
}
