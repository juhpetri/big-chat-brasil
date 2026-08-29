package com.bcb.client.exceptions;

import com.bcb.common.ApiException;
import com.bcb.domain.DocumentType;
import org.springframework.http.HttpStatus;

public class InvalidDocumentFormatException extends ApiException {

    public InvalidDocumentFormatException(String document) {
        super(String.format("Documento inválido: %s", document), HttpStatus.BAD_REQUEST);
    }
}
