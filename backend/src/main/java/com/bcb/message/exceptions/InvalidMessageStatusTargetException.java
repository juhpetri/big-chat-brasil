package com.bcb.message.exceptions;

import com.bcb.common.DomainException;
import com.bcb.domain.MessageStatus;

public class InvalidMessageStatusTargetException extends DomainException {

    public InvalidMessageStatusTargetException(MessageStatus target) {
        super(String.format("Status %s não pode ser definido manualmente — só DELIVERED ou READ.", target));
    }
}
