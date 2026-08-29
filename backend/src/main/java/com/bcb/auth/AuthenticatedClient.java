package com.bcb.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.UUID;

@Setter
@Getter
@Component
@RequestScope
public class AuthenticatedClient {

    private UUID clientId;
 }
