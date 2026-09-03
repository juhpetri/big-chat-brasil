package com.bcb.auth;

import com.bcb.auth.dto.SessionDto;
import com.bcb.client.dto.ClientResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SessionService {

    private static final Duration SESSION_TTL = Duration.ofHours(24);

    private final SessionRepository sessionRepository;

    public void createSession(String token, ClientResponse clientResponse) {
        Session session = new Session();
        session.setToken(token);
        session.setClient(clientResponse.toClient());

        sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public Optional<SessionDto> findByToken(String token) {
        return sessionRepository.findByToken(token)
                .filter(this::isValid)
                .map(session -> SessionDto.builder()
                        .client(session.getClient().toClientResponse())
                        .token(session.getToken())
                        .build());
    }

    private boolean isValid(Session session) {
        if (session.getCreatedAt() != null && session.getCreatedAt().isBefore(LocalDateTime.now().minus(SESSION_TTL))) {
            return false;
        }
        return Boolean.TRUE.equals(session.getClient().getActive());
    }
}
