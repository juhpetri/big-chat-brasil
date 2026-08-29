package com.bcb.message.exceptions;

import com.bcb.common.ApiException;
import com.bcb.domain.MessageStatus;
import org.springframework.http.HttpStatus;

public class InvalidMessageStatusTargetException extends ApiException {

    public InvalidMessageStatusTargetException(MessageStatus target) {
        super(String.format("Status %s não pode ser definido manualmente — só DELIVERED ou READ.", target),
                HttpStatus.BAD_REQUEST);
    }
}
