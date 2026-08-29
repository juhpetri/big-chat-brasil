package com.bcb.client.exceptions;

import com.bcb.common.ApiException;
import org.springframework.http.HttpStatus;

public class ClientInactiveException extends ApiException {

    public ClientInactiveException(String name) {
        super(String.format("O cliente %s está inativo!",name), HttpStatus.FORBIDDEN);
    }
}
