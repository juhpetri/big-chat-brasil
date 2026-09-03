package com.bcb.message;

import com.bcb.client.ClientService;
import com.bcb.client.dto.ClientResponse;
import com.bcb.conversation.Conversation;
import com.bcb.conversation.ConversationService;
import com.bcb.domain.MessageStatus;
import com.bcb.domain.PlanType;
import com.bcb.domain.SenderType;
import com.bcb.message.dto.MessageResponse;
import com.bcb.message.dto.SendMessageRequest;
import com.bcb.message.dto.SendMessageResponse;
import com.bcb.message.exceptions.MessageNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private static final int ESTIMATED_DELIVERY_SECONDS = 3;

    private final MessageRepository messageRepository;
    private final ClientService clientService;
    private final ConversationService conversationService;
    private final MessageQueueService messageQueueService;

    @Transactional
    public SendMessageResponse send(UUID clientId, SendMessageRequest request) {
        ClientResponse client = clientService.chargeForMessage(clientId, request.priority());
        UUID conversationId = conversationService.resolveConversation(clientId, request.recipientId(), request.recipientName());

        Message savedMessage = createMessage(request, conversationId);

        conversationService.touchLastMessageAt(conversationId, savedMessage.getQueuedAt());
        enqueueAfterCommit(savedMessage);

        return toResponse(savedMessage, conversationId, client);
    }

    private void enqueueAfterCommit(Message message) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            messageQueueService.enqueue(message);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messageQueueService.enqueue(message);
            }
        });
    }

    public List<MessageResponse> listByConversation(UUID conversationId) {
        return messageRepository.findByConversation_IdOrderByQueuedAtAsc(conversationId).stream()
                .map(Message::toMessageResponse)
                .toList();
    }

    @Transactional
    public MessageResponse updateStatus(UUID clientId, UUID messageId, MessageStatus newStatus) {
        newStatus.checkManuallySettable();

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        conversationService.assertOwnership(clientId, message.getConversation().getId());

        message.updateStatus(newStatus);
        return messageRepository.save(message).toMessageResponse();
    }

    public Map<UUID, Long> countUnreadGroupedByConversation(List<UUID> conversationIds) {
        return messageRepository.countUnreadGroupedByConversation(conversationIds).stream()
                .collect(Collectors.toMap(MessageRepository.UnreadCount::getConversationId,
                        MessageRepository.UnreadCount::getUnreadCount));
    }

    private Message createMessage(SendMessageRequest request, UUID conversationId) {
        Conversation conversation = conversationService.getConversationReference(conversationId);

        Message message = new Message();
        message.setConversation(conversation);
        message.setContent(request.content());
        message.setPriority(request.priority());
        message.setStatus(MessageStatus.QUEUED);
        message.setCost(request.priority().getCost());
        message.setSentByType(SenderType.CLIENT);
        message.setQueuedAt(LocalDateTime.now());

        return messageRepository.save(message);
    }

    private SendMessageResponse toResponse(Message message, UUID conversationId, ClientResponse client) {
        LocalDateTime estimatedDelivery = message.getQueuedAt().plusSeconds(ESTIMATED_DELIVERY_SECONDS);
        BigDecimal currentBalance = client.planType() == PlanType.PREPAID ? client.balance() : null;

        return new SendMessageResponse(message.getId(), conversationId, message.getStatus(), message.getQueuedAt(),
                estimatedDelivery, message.getCost(), currentBalance);
    }
}
