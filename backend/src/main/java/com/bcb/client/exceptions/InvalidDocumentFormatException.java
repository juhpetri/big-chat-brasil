package com.bcb.client.exceptions;

import com.bcb.common.DomainException;

public class InvalidDocumentFormatException extends DomainException {

    public InvalidDocumentFormatException(String document) {
        super(String.format("Documento inválido: %s", document));
    }
}
