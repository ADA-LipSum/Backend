// src/main/java/com/ada/proj/dto/UserBanStatusResponse.java
package com.ada.proj.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserBanStatusResponse {

    private String userUuid;
    private String nickname;
    private String profileImage;

    private Boolean banned;            // true 제재됨 / false 정상
    private String banReason;
    private LocalDateTime bannedAt;
    private LocalDateTime expiresAt;
}