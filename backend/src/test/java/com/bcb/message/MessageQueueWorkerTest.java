package com.bcb.message;

import com.bcb.domain.MessagePriority;
import com.bcb.domain.MessageStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageQueueWorkerTest {

    @Mock
    private MessageQueueService messageQueueService;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private MessageQueueWorker messageQueueWorker;

    @Test
    void filaVaziaNaoConsultaRepositorio() {
        when(messageQueueService.poll()).thenReturn(null);

        messageQueueWorker.processNext();

        verify(messageRepository, never()).findById(any());
    }

    @Test
    void processaProximaMensagemMarcandoProcessingEDepoisSent() {
        UUID messageId = UUID.randomUUID();
        QueuedMessage queuedMessage = new QueuedMessage(messageId, MessagePriority.NORMAL, LocalDateTime.now(), 0);
        Message message = new Message();
        message.setId(messageId);
        message.setStatus(MessageStatus.QUEUED);

        when(messageQueueService.poll()).thenReturn(queuedMessage);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        messageQueueWorker.processNext();

        assertThat(message.getStatus()).isEqualTo(MessageStatus.SENT);
        assertThat(message.getProcessedAt()).isNotNull();
        verify(messageRepository, times(2)).save(any());
    }

    @Test
    void mensagemNaoEncontradaNoBancoNaoLancaExcecao() {
        UUID messageId = UUID.randomUUID();
        QueuedMessage queuedMessage = new QueuedMessage(messageId, MessagePriority.NORMAL, LocalDateTime.now(), 0);
        when(messageQueueService.poll()).thenReturn(queuedMessage);
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

        messageQueueWorker.processNext();

        verify(messageRepository, never()).save(any());
    }

    @Test
    void falhaAoSalvarRecolocaNaFilaEnquantoNaoAtingeLimiteDeTentativas() {
        UUID messageId = UUID.randomUUID();
        QueuedMessage queuedMessage = new QueuedMessage(messageId, MessagePriority.NORMAL, LocalDateTime.now(), 0);
        Message message = new Message();
        message.setId(messageId);

        when(messageQueueService.poll()).thenReturn(queuedMessage);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageRepository.save(any())).thenThrow(new RuntimeException("banco indisponível"));

        messageQueueWorker.processNext();

        verify(messageQueueService).requeue(queuedMessage);
        assertThat(message.getStatus()).isNotEqualTo(MessageStatus.FAILED);
    }

    @Test
    void esgotarTentativasMarcaMensagemComoFailed() {
        UUID messageId = UUID.randomUUID();
        QueuedMessage ultimaTentativa = new QueuedMessage(messageId, MessagePriority.NORMAL, LocalDateTime.now(), 2);
        Message message = new Message();
        message.setId(messageId);

        when(messageQueueService.poll()).thenReturn(ultimaTentativa);
        when(messageRepository.findById(messageId))
                .thenThrow(new RuntimeException("banco indisponível"))
                .thenReturn(Optional.of(message));

        messageQueueWorker.processNext();

        verify(messageQueueService, never()).requeue(any());
        assertThat(message.getStatus()).isEqualTo(MessageStatus.FAILED);
        assertThat(message.getProcessedAt()).isNotNull();
    }
}
