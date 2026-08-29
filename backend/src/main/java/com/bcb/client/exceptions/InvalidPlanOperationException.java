package com.bcb.client.exceptions;

import com.bcb.common.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidPlanOperationException extends ApiException {

    public InvalidPlanOperationException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
