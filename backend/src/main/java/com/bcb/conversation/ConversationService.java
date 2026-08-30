package com.bcb.conversation;

import com.bcb.client.Client;
import com.bcb.client.ClientService;
import com.bcb.conversation.dto.ConversationSummary;
import com.bcb.conversation.exceptions.ConversationNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ClientService clientService;

    public List<ConversationSummary> listConversations(UUID clientId) {
        return conversationRepository.findByClient_IdOrderByLastMessageAtDesc(clientId).stream()
                .map(Conversation::toSummary)
                .toList();
    }

    public void assertOwnership(UUID clientId, UUID conversationId) {
        findOwnedConversation(clientId, conversationId);
    }

    @Transactional
    public UUID resolveConversation(UUID clientId, String recipientId, String recipientName) {
        return conversationRepository.findByClient_IdAndRecipientId(clientId, recipientId)
                .orElseGet(() -> createConversation(clientId, recipientId, recipientName))
                .getId();
    }

    @Transactional
    public void touchLastMessageAt(UUID conversationId, LocalDateTime lastMessageAt) {
        Conversation conversation = conversationRepository.getReferenceById(conversationId);
        conversation.setLastMessageAt(lastMessageAt);
        conversationRepository.save(conversation);
    }

    public Conversation getConversationReference(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }

    private Conversation createConversation(UUID clientId, String recipientId, String recipientName) {
        Client client = clientService.getClientReference(clientId);

        Conversation conversation = new Conversation();
        conversation.setClient(client);
        conversation.setRecipientId(recipientId);
        conversation.setRecipientName(recipientName);
        return conversationRepository.save(conversation);
    }

    private void findOwnedConversation(UUID clientId, UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        if (!conversation.getClient().getId().equals(clientId)) {
            throw new ConversationNotFoundException(conversationId);
        }

    }


}
