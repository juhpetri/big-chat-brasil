package com.bcb.conversation;

import com.bcb.client.Client;
import com.bcb.conversation.dto.ConversationSummary;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "conversation")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(columnDefinition = "TEXT")
    private String recipientId;

    @Column(columnDefinition = "TEXT")
    private String recipientName;

    private LocalDateTime lastMessageAt;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public ConversationSummary toSummary() {
        return new ConversationSummary(id, recipientId,
                recipientName, lastMessageAt);
    }
}
