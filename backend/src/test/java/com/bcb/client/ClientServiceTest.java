package com.bcb.client;

import com.bcb.billing.BillingService;
import com.bcb.client.dto.ClientResponse;
import com.bcb.client.dto.CreateClientRequest;
import com.bcb.client.exceptions.ClientNotFoundException;
import com.bcb.client.exceptions.DocumentAlreadyExistsException;
import com.bcb.client.exceptions.InvalidPlanOperationException;
import com.bcb.domain.MessagePriority;
import com.bcb.domain.PlanType;
import com.bcb.domain.TransactionType;
import com.bcb.transaction.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private BillingService billingService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private ClientService clientService;

    @Test
    void criaClienteQuandoDocumentoAindaNaoExiste() {
        CreateClientRequest request = new CreateClientRequest("Empresa X", "12345678901",
                PlanType.PREPAID, new BigDecimal("10.00"), null);
        when(clientRepository.existsByDocumentId(any())).thenReturn(false);
        when(clientRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClientResponse response = clientService.createClient(request);

        assertThat(response.name()).isEqualTo("Empresa X");
        assertThat(response.planType()).isEqualTo(PlanType.PREPAID);
    }

    @Test
    void criarClienteComDocumentoJaCadastradoLancaExcecao() {
        CreateClientRequest request = new CreateClientRequest("Empresa X", "12345678901",
                PlanType.PREPAID, new BigDecimal("10.00"), null);
        when(clientRepository.existsByDocumentId(any())).thenReturn(true);

        assertThatThrownBy(() -> clientService.createClient(request))
                .isInstanceOf(DocumentAlreadyExistsException.class);

        verify(clientRepository, never()).save(any());
    }

    @Test
    void buscarClienteInexistenteLancaClientNotFoundException() {
        UUID clientId = UUID.randomUUID();
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.getClientById(clientId))
                .isInstanceOf(ClientNotFoundException.class);
    }

    @Test
    void cobrarMensagemDebitaClienteERegistraTransacao() {
        UUID clientId = UUID.randomUUID();
        Client client = Client.builder().id(clientId).planType(PlanType.PREPAID).active(true).balance(new BigDecimal("10.00")).build();
        when(clientRepository.findByIdForUpdate(clientId)).thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(client);

        clientService.chargeForMessage(clientId, MessagePriority.NORMAL);

        verify(billingService).validateAndCharge(client, MessagePriority.NORMAL);
        verify(transactionService).record(clientId, null, TransactionType.DEBIT, MessagePriority.NORMAL.getCost());
    }

    @Test
    void adicionarCreditoEmClientePospagoLancaExcecao() {
        UUID clientId = UUID.randomUUID();
        Client client = Client.builder().id(clientId).planType(PlanType.POSTPAID).build();
        when(clientRepository.findByIdForUpdate(clientId)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> clientService.addCredit(clientId, new BigDecimal("10.00")))
                .isInstanceOf(InvalidPlanOperationException.class);

        verify(clientRepository, never()).save(any());
    }

    @Test
    void adicionarCreditoEmClientePrepagoSomaAoSaldo() {
        UUID clientId = UUID.randomUUID();
        Client client = Client.builder().id(clientId).planType(PlanType.PREPAID).active(true).balance(new BigDecimal("5.00")).build();
        when(clientRepository.findByIdForUpdate(clientId)).thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(client);

        ClientResponse response = clientService.addCredit(clientId, new BigDecimal("10.00"));

        assertThat(response.balance()).isEqualByComparingTo("15.00");
        verify(transactionService).record(clientId, null, TransactionType.CREDIT, new BigDecimal("10.00"));
    }

    @Test
    void ajustarLimiteEmClientePrepagoLancaExcecao() {
        UUID clientId = UUID.randomUUID();
        Client client = Client.builder().id(clientId).planType(PlanType.PREPAID).build();
        when(clientRepository.findByIdForUpdate(clientId)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> clientService.adjustLimit(clientId, new BigDecimal("100.00")))
                .isInstanceOf(InvalidPlanOperationException.class);
    }

    @Test
    void converterParaMesmoPlanoLancaExcecao() {
        UUID clientId = UUID.randomUUID();
        Client client = Client.builder().id(clientId).planType(PlanType.PREPAID).balance(BigDecimal.ZERO).build();
        when(clientRepository.findByIdForUpdate(clientId)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> clientService.convertPlan(clientId, PlanType.PREPAID, new BigDecimal("10.00")))
                .isInstanceOf(InvalidPlanOperationException.class);

        verify(transactionService, never()).record(any(), any(), any(), any());
    }

    @Test
    void converterDePrepagoParaPospagoRegistraResiduoDoSaldoComoCredito() {
        UUID clientId = UUID.randomUUID();
        Client client = Client.builder().id(clientId).planType(PlanType.PREPAID).active(true).balance(new BigDecimal("3.50")).build();
        when(clientRepository.findByIdForUpdate(clientId)).thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(client);

        ClientResponse response = clientService.convertPlan(clientId, PlanType.POSTPAID, new BigDecimal("50.00"));

        verify(transactionService).record(clientId, null, TransactionType.CREDIT, new BigDecimal("3.50"));
        assertThat(response.planType()).isEqualTo(PlanType.POSTPAID);
        assertThat(client.getBalance()).isNull();
        assertThat(client.getMonthlyLimit()).isEqualByComparingTo("50.00");
        assertThat(client.getMonthlyUsage()).isEqualByComparingTo("0.00");
    }
}
