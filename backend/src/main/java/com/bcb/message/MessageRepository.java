package com.bcb.message;

import com.bcb.domain.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversation_IdOrderByQueuedAtAsc(UUID conversationId);

    List<Message> findByStatusIn(List<MessageStatus> statuses);

    @Query("""
            select m.conversation.id as conversationId, count(m) as unreadCount
            from Message m
            where m.conversation.id in :conversationIds
              and m.sentByType = com.bcb.domain.SenderType.USER
              and m.status <> com.bcb.domain.MessageStatus.READ
            group by m.conversation.id
            """)
    List<UnreadCount> countUnreadGroupedByConversation(@Param("conversationIds") List<UUID> conversationIds);

    interface UnreadCount {
        UUID getConversationId();
        long getUnreadCount();
    }

}
