package com.bcb.auth;

import com.bcb.auth.dto.AuthRequest;
import com.bcb.auth.dto.AuthResponse;
import com.bcb.client.ClientService;
import com.bcb.client.DocumentId;
import com.bcb.client.dto.ClientResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ClientService clientService;
    private final SessionService sessionService;

    public AuthResponse authenticate(AuthRequest authRequest) {
        ClientResponse client = getClientResponse(authRequest.document());

        String token = createSession(client);

        return new AuthResponse(token, client);

    }

    private String createSession(ClientResponse client) {
        String token = UUID.randomUUID().toString();

        sessionService.createSession(token, client);

        return token;
    }

    private ClientResponse getClientResponse(String document) {
        DocumentId documentId = DocumentId.of(document);

        ClientResponse client = clientService.getClientByDocument(documentId);
        client.assertActive();

        return client;
    }
}
