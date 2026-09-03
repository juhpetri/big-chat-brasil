package com.bcb.client;

import com.bcb.billing.BillingService;
import com.bcb.client.dto.ClientResponse;
import com.bcb.client.dto.CreateClientRequest;
import com.bcb.client.exceptions.ClientNotFoundException;
import com.bcb.client.exceptions.DocumentAlreadyExistsException;
import com.bcb.domain.MessagePriority;
import com.bcb.domain.PlanType;
import com.bcb.domain.TransactionType;
import com.bcb.transaction.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final BillingService billingService;
    private final TransactionService transactionService;

    public ClientResponse createClient(CreateClientRequest clientRequest) {
        Client client = clientRequest.toClient();
        validateClientExistent(client.getDocumentId());

        Client clientSaved = clientRepository.save(client);

        return clientSaved.toClientResponse();
    }

    public ClientResponse getClientById(UUID clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        return client.toClientResponse();
    }

    public Client getClientReference(UUID clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));
    }

    public ClientResponse getClientByDocument(DocumentId documentId) {
        Client client = clientRepository.findByDocumentId(documentId)
                .orElseThrow(ClientNotFoundException::new);

        return client.toClientResponse();
    }

    @Transactional
    public ClientResponse chargeForMessage(UUID clientId, MessagePriority priority) {
        Client client = clientRepository.findByIdForUpdate(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        billingService.validateAndCharge(client, priority);

        Client chargedClient = clientRepository.save(client);
        transactionService.record(clientId, null, TransactionType.DEBIT, priority.getCost(),
                "Envio de mensagem " + priority.getPriorityLabel());
        return chargedClient.toClientResponse();
    }

    @Transactional
    public ClientResponse addCredit(UUID clientId, BigDecimal amount) {
        Client client = clientRepository.findByIdForUpdate(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        client.checkIsPrepaid();

        client.setBalance(client.getBalance().add(amount));
        Client saved = clientRepository.save(client);
        transactionService.record(clientId, null, TransactionType.CREDIT, amount, "Crédito adicionado");
        return saved.toClientResponse();
    }

    @Transactional
    public ClientResponse adjustLimit(UUID clientId, BigDecimal newLimit) {
        Client client = clientRepository.findByIdForUpdate(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        client.checkIsPostpaid();

        client.setMonthlyLimit(newLimit);
        Client saved = clientRepository.save(client);
        return saved.toClientResponse();
    }

    @Transactional
    public ClientResponse convertPlan(UUID clientId, PlanType newPlanType, BigDecimal initialValue) {
        Client client = clientRepository.findByIdForUpdate(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        client.checkAlreadyPlanType(newPlanType);

        BigDecimal newPlanValue = newPlanType.isPrePaid()
                ? convertToPrepaid(client, newPlanType, initialValue)
                : convertToPostpaid(client, newPlanType, initialValue);

        client.convertToPlanType(newPlanValue, newPlanType);

        Client saved = clientRepository.save(client);
        return saved.toClientResponse();
    }

    private BigDecimal convertToPrepaid(Client client, PlanType newPlanType, BigDecimal initialValue) {
        recordIfPositive(client.getId(), TransactionType.DEBIT, client.getResidualValue(),
                "Limite mensal não utilizado zerado na conversão de plano para " + newPlanType.getDescription());
        recordIfPositive(client.getId(), TransactionType.CREDIT, initialValue,
                "Saldo inicial da conversão de plano para " + newPlanType.getDescription());
        return initialValue;
    }

    private BigDecimal convertToPostpaid(Client client, PlanType newPlanType, BigDecimal initialValue) {
        BigDecimal carriedResidual = client.carriesResidualTo(newPlanType) ? client.getResidualValue() : BigDecimal.ZERO;
        recordIfPositive(client.getId(), TransactionType.CREDIT, carriedResidual,
                "Conversão de plano para " + newPlanType.getDescription());
        return initialValue.add(carriedResidual);
    }

    private void recordIfPositive(UUID clientId, TransactionType type, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            transactionService.record(clientId, null, type, amount, description);
        }
    }

    private void validateClientExistent(DocumentId documentId) {
        boolean clientExists = clientRepository.existsByDocumentId(documentId);
        if (clientExists) {
            throw new DocumentAlreadyExistsException();
        }
    }
}
