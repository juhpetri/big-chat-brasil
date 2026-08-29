package com.bcb.domain;

import com.bcb.client.exceptions.InvalidDocumentFormatException;

public enum DocumentType {
    CPF,
    CNPJ;

    public static DocumentType identifyByLength(String digitsOnly) {
        return switch (digitsOnly.length()) {
            case 11 -> CPF;
            case 14 -> CNPJ;
            default -> throw new InvalidDocumentFormatException(digitsOnly);
        };
    }

}
