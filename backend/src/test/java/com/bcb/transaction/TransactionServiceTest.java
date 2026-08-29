package com.bcb.transaction;

import com.bcb.domain.TransactionType;
import com.bcb.transaction.dto.TransactionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void registraTransacaoComOsDadosInformados() {
        UUID clientId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        transactionService.record(clientId, messageId, TransactionType.DEBIT, new BigDecimal("0.25"));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());

        Transaction saved = captor.getValue();
        assertThat(saved.getClientId()).isEqualTo(clientId);
        assertThat(saved.getMessageId()).isEqualTo(messageId);
        assertThat(saved.getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(saved.getAmount()).isEqualByComparingTo("0.25");
    }

    @Test
    void listaTransacoesDoClienteMaisRecentePrimeiro() {
        UUID clientId = UUID.randomUUID();
        Transaction transaction = new Transaction();
        transaction.setClientId(clientId);
        transaction.setType(TransactionType.CREDIT);
        transaction.setAmount(new BigDecimal("10.00"));
        when(transactionRepository.findByClientIdOrderByTimestampDesc(clientId)).thenReturn(List.of(transaction));

        List<TransactionResponse> result = transactionService.listByClient(clientId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo(TransactionType.CREDIT);
    }
}
