package com.bcb.message;

import com.bcb.domain.MessageStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageQueueWorker {

    // Tentativas antes de desistir e marcar FAILED. Sem backoff: a mensagem volta pra fila com o
    // mesmo queuedAt, então é reprocessada no próximo tick (3s) — suficiente pra falha transitória
    // de conexão com o banco; não tenta resolver falha permanente (mensagem inválida, etc.).
    private static final int MAX_ATTEMPTS = 3;

    private final MessageQueueService messageQueueService;
    private final MessageRepository messageRepository;

    @Scheduled(fixedDelay = 3000)
    public void processNext() {
        QueuedMessage queuedMessage = messageQueueService.poll();
        if (queuedMessage == null) {
            return;
        }

        try {
            messageRepository.findById(queuedMessage.messageId()).ifPresentOrElse(
                    this::markAsSent,
                    () -> log.warn("Mensagem {} não encontrada no banco, descartada da fila", queuedMessage.messageId()));
        } catch (Exception exception) {
            handleFailure(queuedMessage, exception);
        }
    }

    private void markAsSent(Message message) {
        message.setStatus(MessageStatus.PROCESSING);
        messageRepository.save(message);

        message.setStatus(MessageStatus.SENT);
        message.setProcessedAt(LocalDateTime.now());
        messageRepository.save(message);
    }

    private void handleFailure(QueuedMessage queuedMessage, Exception exception) {
        if (queuedMessage.attempts() + 1 < MAX_ATTEMPTS) {
            log.warn("Falha ao processar mensagem {} (tentativa {}/{}), recolocando na fila",
                    queuedMessage.messageId(), queuedMessage.attempts() + 1, MAX_ATTEMPTS, exception);
            messageQueueService.requeue(queuedMessage);
            return;
        }

        log.error("Mensagem {} falhou {} vezes, marcando como FAILED",
                queuedMessage.messageId(), MAX_ATTEMPTS, exception);
        markAsFailed(queuedMessage);
    }

    private void markAsFailed(QueuedMessage queuedMessage) {
        try {
            messageRepository.findById(queuedMessage.messageId()).ifPresent(message -> {
                message.setStatus(MessageStatus.FAILED);
                message.setProcessedAt(LocalDateTime.now());
                messageRepository.save(message);
            });
        } catch (Exception exception) {
            log.error("Não foi possível marcar mensagem {} como FAILED", queuedMessage.messageId(), exception);
        }
    }
}
