package com.bcb.message;

import com.bcb.domain.MessagePriority;
import com.bcb.domain.MessageStatus;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Fila de prioridade em memória: URGENT sempre sai antes de NORMAL: entre mensagens da mesma
 * prioridade, FIFO por {@code queuedAt} ({@link MessagePriority#priority()} retorna 0 pra URGENT,
 * 1 pra NORMAL — menor valor primeiro no {@link PriorityBlockingQueue}).
 */
@Service
@RequiredArgsConstructor
public class MessageQueueService {

    private static final Comparator<QueuedMessage> QUEUE_ORDER =
            Comparator.comparingInt((QueuedMessage m) -> m.priority().priority())
                    .thenComparing(QueuedMessage::queuedAt);

    private final MessageRepository messageRepository;

    private final PriorityBlockingQueue<QueuedMessage> queue = new PriorityBlockingQueue<>(11, QUEUE_ORDER);

    // Recarrega QUEUED/PROCESSING do banco no boot: a fila em memória se perde a cada restart,
    // mas as mensagens continuam persistidas — sem isso, mensagens em trânsito no momento de um
    // deploy/restart nunca mais seriam processadas.
    @PostConstruct
    void rehydrate() {
        messageRepository.findByStatusIn(List.of(MessageStatus.QUEUED, MessageStatus.PROCESSING))
                .forEach(message -> queue.offer(toQueuedMessage(message)));
    }

    public void enqueue(Message message) {
        queue.offer(toQueuedMessage(message));
    }

    public QueuedMessage poll() {
        return queue.poll();
    }

    // Retentativa: mantém queuedAt original (não pula a fila) e incrementa attempts.
    // Quem decide quando desistir (MessageQueueWorker) sabe o limite de tentativas.
    public void requeue(QueuedMessage message) {
        queue.offer(message.nextAttempt());
    }

    private QueuedMessage toQueuedMessage(Message message) {
        return new QueuedMessage(message.getId(), message.getPriority(), message.getQueuedAt(), 0);
    }
}
