package com.ada.proj.dto;

import com.ada.proj.enums.Role;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthMeResponse {

    private String uuid;
    private Role role;
    private String userRealname;
    private String userNickname;
    private String profileImage;

    @JsonProperty("isFirstLogin")
    private boolean firstLogin;
}
