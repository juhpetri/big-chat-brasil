package com.bcb.client.exceptions;

import com.bcb.common.DomainException;

public class InvalidPlanOperationException extends DomainException {

    public InvalidPlanOperationException(String message) {
        super(message);
    }
}
