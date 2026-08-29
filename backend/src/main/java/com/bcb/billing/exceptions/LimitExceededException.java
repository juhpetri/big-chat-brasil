package com.bcb.billing.exceptions;

import com.bcb.common.ApiException;
import org.springframework.http.HttpStatus;

public class LimitExceededException extends ApiException {
    public LimitExceededException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
