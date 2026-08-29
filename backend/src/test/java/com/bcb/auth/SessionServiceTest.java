package com.bcb.auth;

import com.bcb.auth.dto.SessionDto;
import com.bcb.client.Client;
import com.bcb.client.DocumentId;
import com.bcb.client.dto.ClientResponse;
import com.bcb.domain.DocumentType;
import com.bcb.domain.PlanType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private SessionService sessionService;

    @Test
    void criaSessaoPersistindoTokenEClienteAssociado() {
        ClientResponse client = clientResponse();

        sessionService.createSession("token-123", client);

        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());

        Session saved = captor.getValue();
        assertThat(saved.getToken()).isEqualTo("token-123");
        assertThat(saved.getClient().getId()).isEqualTo(client.id());
    }

    @Test
    void tokenValidoRetornaSessionDtoComCliente() {
        Session session = new Session();
        session.setToken("token-123");
        session.setClient(clientResponse().toClient());
        when(sessionRepository.findByToken("token-123")).thenReturn(Optional.of(session));

        Optional<SessionDto> result = sessionService.findByToken("token-123");

        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo("token-123");
    }

    @Test
    void tokenInexistenteRetornaOptionalVazio() {
        when(sessionRepository.findByToken("invalido")).thenReturn(Optional.empty());

        assertThat(sessionService.findByToken("invalido")).isEmpty();
    }

    private ClientResponse clientResponse() {
        return new ClientResponse(UUID.randomUUID(), "Cliente Teste",
                new DocumentId("12345678901", DocumentType.CPF), PlanType.PREPAID,
                new BigDecimal("10.00"), null, true);
    }
}
