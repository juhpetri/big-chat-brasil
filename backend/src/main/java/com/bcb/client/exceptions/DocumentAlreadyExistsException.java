package com.bcb.client.exceptions;

import com.bcb.common.ApiException;
import org.springframework.http.HttpStatus;

public class DocumentAlreadyExistsException extends ApiException {

    public DocumentAlreadyExistsException() {
        super("Documento já cadastrado!", HttpStatus.CONFLICT);
    }
}
