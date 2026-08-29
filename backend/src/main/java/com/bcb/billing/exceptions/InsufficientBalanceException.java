package com.bcb.billing.exceptions;

import com.bcb.common.ApiException;
import org.springframework.http.HttpStatus;

public class InsufficientBalanceException extends ApiException {
    public InsufficientBalanceException(String message) {
        super(message, HttpStatus.PAYMENT_REQUIRED);
    }
}
