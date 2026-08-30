package com.bcb.message;

import com.bcb.domain.MessageStatus;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

@Service
@RequiredArgsConstructor
public class MessageQueueService {

    private static final Comparator<QueuedMessage> QUEUE_ORDER =
            Comparator.comparingInt((QueuedMessage m) -> m.priority().priority())
                    .thenComparing(QueuedMessage::queuedAt);

    private final MessageRepository messageRepository;

    private final PriorityBlockingQueue<QueuedMessage> queue = new PriorityBlockingQueue<>(11, QUEUE_ORDER);

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

    public void requeue(QueuedMessage message) {
        queue.offer(message.nextAttempt());
    }

    private QueuedMessage toQueuedMessage(Message message) {
        return new QueuedMessage(message.getId(), message.getPriority(), message.getQueuedAt(), 0);
    }
}
