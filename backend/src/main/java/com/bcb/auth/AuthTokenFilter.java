package com.bcb.auth;

import com.bcb.auth.dto.SessionDto;
import com.bcb.common.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    private static final String METHOD_OPTIONS = "OPTIONS";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/auth", "/api/ping", "/swagger-ui", "/v3/api-docs", "/actuator");


    private final SessionService sessionService;
    private final AuthenticatedClient authenticatedClient;
    private final ObjectMapper objectMapper;

    public AuthTokenFilter(SessionService sessionService, AuthenticatedClient authenticatedClient, ObjectMapper objectMapper) {
        this.sessionService = sessionService;
        this.authenticatedClient = authenticatedClient;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(response, "Token ausente");
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        Optional<SessionDto> optSession = sessionService.findByToken(token);

        if (optSession.isEmpty()) {
            writeUnauthorized(response, "Token inválido");
            return;
        }

        authenticatedClient.setClientId(optSession.get().getClient().id());
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new ErrorResponse("unauthorized", message));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)  {
        if (METHOD_OPTIONS.equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (PUBLIC_PREFIXES.stream().anyMatch(path::startsWith)) {
            return true;
        }

         return "POST".equalsIgnoreCase(request.getMethod()) && "/api/clients".equals(path);
    }
}
