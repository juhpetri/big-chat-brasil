package com.bcb.message;

import com.bcb.conversation.Conversation;
import com.bcb.domain.MessagePriority;
import com.bcb.domain.MessageStatus;
import com.bcb.domain.SenderType;
import com.bcb.message.dto.MessageResponse;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "TEXT")
    private MessagePriority priority;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "TEXT")
    private MessageStatus status;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "TEXT")
    private SenderType sentByType;

    @Column(precision = 10, scale = 2)
    private BigDecimal cost;

    private LocalDateTime createdAt;

    private LocalDateTime queuedAt;

    private LocalDateTime processedAt;

    @Column(columnDefinition = "TEXT")
    private String content;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public MessageResponse toMessageResponse() {
        return new MessageResponse(id, content, priority,
                status, cost, sentByType, queuedAt, processedAt);
    }
}
