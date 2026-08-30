package com.bcb.message.exceptions;

import com.bcb.common.DomainException;
import com.bcb.domain.MessageStatus;

public class InvalidMessageStatusTransitionException extends DomainException {

    public InvalidMessageStatusTransitionException(MessageStatus from, MessageStatus to) {
        super(String.format("Não é possível marcar como %s uma mensagem com status %s.", to, from));
    }
}
