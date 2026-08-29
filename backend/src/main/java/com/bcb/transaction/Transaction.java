package com.bcb.transaction;

import com.bcb.domain.TransactionType;
import com.bcb.transaction.dto.TransactionResponse;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "message_id")
    private UUID messageId;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "TEXT")
    private TransactionType type;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    private LocalDateTime timestamp;

    @PrePersist
    void onCreate() {
        this.timestamp = LocalDateTime.now();
    }

    public TransactionResponse toResponse() {
        return new TransactionResponse(id, messageId, type, amount, timestamp);
    }
}
