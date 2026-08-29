package com.bcb.client;

import com.bcb.domain.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public record DocumentId(
        @Column(unique = true, columnDefinition = "TEXT")
        String document,

        @Enumerated(EnumType.STRING)
        @Column(columnDefinition = "TEXT")
        DocumentType documentType) {

    public static DocumentId of(String rawValue) {
        String digitsOnly = onlyDigits(rawValue);
        return new DocumentId(digitsOnly, DocumentType.identifyByLength(digitsOnly));
    }

    private static String onlyDigits(String rawValue) {
        return rawValue.replaceAll("\\D", "");
    }
}
