package com.bcb.config;

import com.bcb.client.Client;
import com.bcb.client.ClientRepository;
import com.bcb.client.ClientService;
import com.bcb.client.DocumentId;
import com.bcb.client.dto.ClientResponse;
import com.bcb.auth.SessionService;
import com.bcb.conversation.Conversation;
import com.bcb.conversation.ConversationRepository;
import com.bcb.domain.DocumentType;
import com.bcb.domain.MessagePriority;
import com.bcb.domain.MessageStatus;
import com.bcb.domain.PlanType;
import com.bcb.domain.SenderType;
import com.bcb.message.Message;
import com.bcb.message.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@Profile("seed")
@RequiredArgsConstructor
public class SeedDataRunner implements CommandLineRunner {

    private static final String PREPAID_TOKEN = "seed-token-prepaid";
    private static final String POSTPAID_TOKEN = "seed-token-postpaid";

    private final ClientRepository clientRepository;
    private final ClientService clientService;
    private final SessionService sessionService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Override
    public void run(String... args) {
        if (clientRepository.count() > 0) {
            log.info("Seed pulado — já existe dado no banco.");
            return;
        }

        Client prepaidClient = seedPrepaidClient();
        Client postpaidClient = seedPostpaidClient();

        seedConversationsAndMessages(prepaidClient, "recipient-ana-cliente", "Ana Cliente");
        seedConversationsAndMessages(postpaidClient, "recipient-bruno-comprador", "Bruno Comprador");

        log.info("Seed concluído. Tokens prontos pra usar no Swagger (Authorize): "
                + "'{}' (pré-pago, R$50 de saldo) e '{}' (pós-pago, limite R$200).", PREPAID_TOKEN, POSTPAID_TOKEN);
    }

    private Client seedPrepaidClient() {
        Client client = Client.builder()
                .name("Loja da Ana")
                .documentId(DocumentId.of("52998224725"))
                .planType(PlanType.PREPAID)
                .active(true)
                .balance(BigDecimal.valueOf(50.00))
                .build();

        client = clientRepository.save(client);
        createSession(client, PREPAID_TOKEN);
        return client;
    }

    private Client seedPostpaidClient() {
        Client client = Client.builder()
                .name("Mercado Silva Ltda")
                .documentId(DocumentId.of("11222333000181"))
                .planType(PlanType.POSTPAID)
                .active(true)
                .monthlyLimit(BigDecimal.valueOf(200.00))
                .monthlyUsage(BigDecimal.valueOf(35.00))
                .build();

        client = clientRepository.save(client);
        createSession(client, POSTPAID_TOKEN);
        return client;
    }

    private void createSession(Client client, String token) {
        ClientResponse clientResponse = clientService.getClientById(client.getId());
        sessionService.createSession(token, clientResponse);
    }

    private void seedConversationsAndMessages(Client client, String recipientId, String recipientName) {
        Conversation conversation = new Conversation();
        conversation.setClient(client);
        conversation.setRecipientId(recipientId);
        conversation.setRecipientName(recipientName);
        conversation.setLastMessageAt(LocalDateTime.now().minusMinutes(5));
        conversation = conversationRepository.save(conversation);

        messageRepository.save(buildMessage(conversation, "Olá! Meu pedido já foi enviado?",
                MessagePriority.NORMAL, MessageStatus.READ, SenderType.CLIENT, 20));
        messageRepository.save(buildMessage(conversation, "Sim, saiu pra entrega hoje de manhã.",
                MessagePriority.NORMAL, MessageStatus.DELIVERED, SenderType.USER, 18));
        messageRepository.save(buildMessage(conversation, "Preciso urgente de confirmação do pagamento!",
                MessagePriority.URGENT, MessageStatus.SENT, SenderType.CLIENT, 5));
    }

    private Message buildMessage(Conversation conversation, String content, MessagePriority priority,
                                  MessageStatus status, SenderType sentByType, int minutesAgo) {
        LocalDateTime queuedAt = LocalDateTime.now().minusMinutes(minutesAgo);

        Message message = new Message();
        message.setConversation(conversation);
        message.setContent(content);
        message.setPriority(priority);
        message.setStatus(status);
        message.setSentByType(sentByType);
        message.setCost(priority.getCost());
        message.setQueuedAt(queuedAt);
        message.setProcessedAt(queuedAt.plusSeconds(3));
        return message;
    }
}
