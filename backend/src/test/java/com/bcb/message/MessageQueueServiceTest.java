package com.bcb.message;

import com.bcb.domain.MessagePriority;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MessageQueueServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Test
    void urgenteFuraFilaDeMensagensNormaisAindaNaoProcessadas() {
        MessageQueueService queueService = new MessageQueueService(messageRepository);

        Message normal1 = message(MessagePriority.NORMAL, LocalDateTime.of(2026, 1, 1, 10, 0, 0));
        Message normal2 = message(MessagePriority.NORMAL, LocalDateTime.of(2026, 1, 1, 10, 0, 1));
        Message urgente = message(MessagePriority.URGENT, LocalDateTime.of(2026, 1, 1, 10, 0, 2));
        Message normal3 = message(MessagePriority.NORMAL, LocalDateTime.of(2026, 1, 1, 10, 0, 3));

        queueService.enqueue(normal1);
        queueService.enqueue(normal2);
        queueService.enqueue(urgente);
        queueService.enqueue(normal3);

        assertThat(queueService.poll().messageId()).isEqualTo(urgente.getId());
        assertThat(queueService.poll().messageId()).isEqualTo(normal1.getId());
        assertThat(queueService.poll().messageId()).isEqualTo(normal2.getId());
        assertThat(queueService.poll().messageId()).isEqualTo(normal3.getId());
    }

    @Test
    void mesmaPrioridadeMantemOrdemDeChegada() {
        MessageQueueService queueService = new MessageQueueService(messageRepository);

        Message primeira = message(MessagePriority.NORMAL, LocalDateTime.of(2026, 1, 1, 9, 0, 0));
        Message segunda = message(MessagePriority.NORMAL, LocalDateTime.of(2026, 1, 1, 9, 0, 5));

        queueService.enqueue(segunda);
        queueService.enqueue(primeira);

        assertThat(queueService.poll().messageId()).isEqualTo(primeira.getId());
        assertThat(queueService.poll().messageId()).isEqualTo(segunda.getId());
    }

    @Test
    void requeueIncrementaAttemptsEMantemAMensagemNaFila() {
        MessageQueueService queueService = new MessageQueueService(messageRepository);
        Message message = message(MessagePriority.NORMAL, LocalDateTime.of(2026, 1, 1, 10, 0, 0));
        queueService.enqueue(message);
        QueuedMessage polled = queueService.poll();

        queueService.requeue(polled);

        QueuedMessage requeued = queueService.poll();
        assertThat(requeued.messageId()).isEqualTo(message.getId());
        assertThat(requeued.attempts()).isEqualTo(1);
    }

    private Message message(MessagePriority priority, LocalDateTime queuedAt) {
        Message message = new Message();
        message.setId(UUID.randomUUID());
        message.setPriority(priority);
        message.setQueuedAt(queuedAt);
        return message;
    }
}
