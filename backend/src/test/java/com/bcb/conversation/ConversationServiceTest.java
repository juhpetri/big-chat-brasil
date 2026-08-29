package com.bcb.conversation;

import com.bcb.client.Client;
import com.bcb.client.ClientService;
import com.bcb.conversation.dto.ConversationSummary;
import com.bcb.conversation.exceptions.ConversationNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ClientService clientService;

    @InjectMocks
    private ConversationService conversationService;

    @Test
    void listaConversasComoResumoOrdenadasPelaUltimaMensagem() {
        UUID clientId = UUID.randomUUID();
        Conversation conversation = conversation(clientId, UUID.randomUUID());
        when(conversationRepository.findByClient_IdOrderByLastMessageAtDesc(clientId)).thenReturn(List.of(conversation));

        List<ConversationSummary> result = conversationService.listConversations(clientId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(conversation.getId());
    }

    @Test
    void assertOwnershipNaoLancaQuandoConversaPertenceAoCliente() {
        UUID clientId = UUID.randomUUID();
        Conversation conversation = conversation(clientId, UUID.randomUUID());
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

        conversationService.assertOwnership(clientId, conversation.getId());
    }

    @Test
    void assertOwnershipLancaQuandoConversaPertenceAOutroCliente() {
        UUID donoReal = UUID.randomUUID();
        Conversation conversation = conversation(donoReal, UUID.randomUUID());
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

        UUID outroCliente = UUID.randomUUID();
        assertThatThrownBy(() -> conversationService.assertOwnership(outroCliente, conversation.getId()))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    @Test
    void assertOwnershipLancaQuandoConversaNaoExiste() {
        UUID conversationId = UUID.randomUUID();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.assertOwnership(UUID.randomUUID(), conversationId))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    @Test
    void resolveConversationRetornaIdDaConversaExistente() {
        UUID clientId = UUID.randomUUID();
        Conversation conversation = conversation(clientId, UUID.randomUUID());
        when(conversationRepository.findByClient_IdAndRecipientId(clientId, "recipient-1"))
                .thenReturn(Optional.of(conversation));

        UUID result = conversationService.resolveConversation(clientId, "recipient-1", "Fulano");

        assertThat(result).isEqualTo(conversation.getId());
    }

    @Test
    void resolveConversationCriaNovaConversaQuandoNaoExiste() {
        UUID clientId = UUID.randomUUID();
        Client client = Client.builder().id(clientId).build();
        when(conversationRepository.findByClient_IdAndRecipientId(clientId, "recipient-1")).thenReturn(Optional.empty());
        when(clientService.getClientReference(clientId)).thenReturn(client);
        when(conversationRepository.save(any())).thenAnswer(invocation -> {
            Conversation saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        UUID result = conversationService.resolveConversation(clientId, "recipient-1", "Fulano");

        assertThat(result).isNotNull();
        verify(conversationRepository).save(any());
    }

    @Test
    void getConversationReferenceLancaQuandoNaoExiste() {
        UUID conversationId = UUID.randomUUID();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.getConversationReference(conversationId))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    private Conversation conversation(UUID clientId, UUID conversationId) {
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setClient(Client.builder().id(clientId).build());
        conversation.setRecipientId("recipient-1");
        conversation.setRecipientName("Fulano");
        conversation.setLastMessageAt(LocalDateTime.now());
        return conversation;
    }
}
