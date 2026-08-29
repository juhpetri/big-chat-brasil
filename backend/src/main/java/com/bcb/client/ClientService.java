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
        transactionService.record(clientId, null, TransactionType.DEBIT, priority.getCost());
        return chargedClient.toClientResponse();
    }

    @Transactional
    public ClientResponse addCredit(UUID clientId, BigDecimal amount) {
        Client client = clientRepository.findByIdForUpdate(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        if (client.getPlanType() != PlanType.PREPAID) {
            throw new InvalidPlanOperationException("Só é possível adicionar crédito a clientes PREPAID.");
        }

        client.setBalance(client.getBalance().add(amount));
        Client saved = clientRepository.save(client);
        transactionService.record(clientId, null, TransactionType.CREDIT, amount);
        return saved.toClientResponse();
    }

    @Transactional
    public ClientResponse adjustLimit(UUID clientId, BigDecimal newLimit) {
        Client client = clientRepository.findByIdForUpdate(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        if (client.getPlanType() != PlanType.POSTPAID) {
            throw new InvalidPlanOperationException("Só é possível ajustar limite de clientes POSTPAID.");
        }

        client.setMonthlyLimit(newLimit);
        Client saved = clientRepository.save(client);
        return saved.toClientResponse();
    }

    @Transactional
    public ClientResponse convertPlan(UUID clientId, PlanType newPlanType, BigDecimal initialValue) {
        Client client = clientRepository.findByIdForUpdate(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        if (client.getPlanType() == newPlanType) {
            throw new InvalidPlanOperationException("Cliente já está no plano " + newPlanType + ".");
        }

        BigDecimal residual = client.getPlanType() == PlanType.PREPAID
                ? client.getBalance()
                : client.getMonthlyLimit().subtract(client.getMonthlyUsage());
        transactionService.record(clientId, null, TransactionType.CREDIT, residual);

        client.setPlanType(newPlanType);
        if (newPlanType == PlanType.PREPAID) {
            client.setBalance(initialValue);
            client.setMonthlyLimit(null);
            client.setMonthlyUsage(null);
        } else {
            client.setMonthlyLimit(initialValue);
            client.setMonthlyUsage(BigDecimal.ZERO);
            client.setBalance(null);
        }

        Client saved = clientRepository.save(client);
        return saved.toClientResponse();
    }

    private void validateClientExistent(DocumentId documentId) {
        boolean clientExists = clientRepository.existsByDocumentId(documentId);
        if (clientExists) {
            throw new DocumentAlreadyExistsException();
        }
    }
}
