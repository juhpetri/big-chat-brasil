package com.bcb.conversation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findByClient_IdOrderByLastMessageAtDesc(UUID clientId);

    Optional<Conversation> findByClient_IdAndRecipientId(UUID clientId, String recipientId);
}
