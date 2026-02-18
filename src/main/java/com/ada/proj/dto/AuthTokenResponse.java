package com.ada.proj.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthTokenResponse {

    private String tokenType;
    private String accessToken;
    private long expiresIn;
}
