package com.bcb.client.exceptions;

import com.bcb.common.ApiException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ClientNotFoundException extends ApiException {

    public ClientNotFoundException(UUID clientId) {
        super(String.format("Cliente com id: %s não encontrado!", clientId), HttpStatus.NOT_FOUND);
    }

    public ClientNotFoundException() {
        super("Cliente não encontrado para o documento informado!", HttpStatus.NOT_FOUND);
    }
}
