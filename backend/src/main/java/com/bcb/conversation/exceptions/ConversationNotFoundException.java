package com.bcb.conversation.exceptions;

import com.bcb.common.ApiException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ConversationNotFoundException extends ApiException {

    public ConversationNotFoundException(UUID conversationId) {
        super(String.format("Conversa com id: %s não encontrada!", conversationId), HttpStatus.NOT_FOUND);
    }
}
