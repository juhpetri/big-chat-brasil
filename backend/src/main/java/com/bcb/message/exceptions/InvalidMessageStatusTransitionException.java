package com.bcb.message.exceptions;

import com.bcb.common.ApiException;
import com.bcb.domain.MessageStatus;
import org.springframework.http.HttpStatus;

public class InvalidMessageStatusTransitionException extends ApiException {

    public InvalidMessageStatusTransitionException(MessageStatus from, MessageStatus to) {
        super(String.format("Não é possível marcar como %s uma mensagem com status %s.", to, from),
                HttpStatus.BAD_REQUEST);
    }
}
