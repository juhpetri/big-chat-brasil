package com.bcb.transaction;

import com.bcb.domain.TransactionType;
import com.bcb.transaction.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public void record(UUID clientId, UUID messageId, TransactionType type, BigDecimal amount, String description) {
        Transaction transaction = new Transaction();
        transaction.setClientId(clientId);
        transaction.setMessageId(messageId);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transactionRepository.save(transaction);
    }

    public List<TransactionResponse> listByClient(UUID clientId) {
        return transactionRepository.findByClientIdOrderByTimestampDesc(clientId).stream()
                .map(Transaction::toResponse)
                .toList();
    }
}
