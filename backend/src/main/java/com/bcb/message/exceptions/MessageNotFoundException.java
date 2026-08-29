package com.bcb.message.exceptions;

import com.bcb.common.ApiException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class MessageNotFoundException extends ApiException {

    public MessageNotFoundException(UUID messageId) {
        super(String.format("Mensagem com id: %s não encontrada!", messageId), HttpStatus.NOT_FOUND);
    }
}
