package com.bcb.auth;

import com.bcb.auth.dto.AuthRequest;
import com.bcb.auth.dto.AuthResponse;
import com.bcb.client.ClientService;
import com.bcb.client.DocumentId;
import com.bcb.client.dto.ClientResponse;
import com.bcb.client.exceptions.ClientInactiveException;
import com.bcb.client.exceptions.ClientNotFoundException;
import com.bcb.domain.DocumentType;
import com.bcb.domain.PlanType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private ClientService clientService;

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private AuthService authService;

    @Test
    void clienteAtivoAutenticaERecebeTokenComSessaoCriada() {
        ClientResponse client = clientResponse(true);
        when(clientService.getClientByDocument(new DocumentId("12345678901", DocumentType.CPF))).thenReturn(client);

        AuthResponse response = authService.authenticate(new AuthRequest("123.456.789-01"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.client()).isEqualTo(client);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(sessionService).createSession(tokenCaptor.capture(), any());
        assertThat(tokenCaptor.getValue()).isEqualTo(response.token());
    }

    @Test
    void clienteInativoBloqueiaSemCriarSessao() {
        ClientResponse client = clientResponse(false);
        when(clientService.getClientByDocument(any())).thenReturn(client);

        assertThatThrownBy(() -> authService.authenticate(new AuthRequest("12345678901")))
                .isInstanceOf(ClientInactiveException.class);

        verify(sessionService, never()).createSession(any(), any());
    }

    @Test
    void documentoNaoCadastradoPropagaExcecaoSemCriarSessao() {
        when(clientService.getClientByDocument(any())).thenThrow(new ClientNotFoundException());

        assertThatThrownBy(() -> authService.authenticate(new AuthRequest("12345678901")))
                .isInstanceOf(ClientNotFoundException.class);

        verify(sessionService, never()).createSession(any(), any());
    }

    private ClientResponse clientResponse(boolean active) {
        return new ClientResponse(UUID.randomUUID(), "Cliente Teste",
                new DocumentId("12345678901", DocumentType.CPF), PlanType.PREPAID,
                new BigDecimal("10.00"), null, active);
    }
}
