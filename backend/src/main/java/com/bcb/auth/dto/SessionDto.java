package com.bcb.auth.dto;

import com.bcb.client.dto.ClientResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionDto {

    private String token;

    private ClientResponse client;
}
