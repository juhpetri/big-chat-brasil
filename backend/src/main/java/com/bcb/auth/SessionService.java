package com.bcb.auth;

import com.bcb.auth.dto.SessionDto;
import com.bcb.client.dto.ClientResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;

    public void createSession(String token, ClientResponse clientResponse) {
        Session session = new Session();
        session.setToken(token);
        session.setClient(clientResponse.toClient());

        sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public Optional<SessionDto> findByToken(String token) {
        return sessionRepository.findByToken(token).map(session -> {
            return SessionDto.builder()
                    .client(session.getClient().toClientResponse())
                    .token(session.getToken())
                    .build();
        });
    }
}
