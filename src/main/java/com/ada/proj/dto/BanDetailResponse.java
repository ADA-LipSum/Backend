// src/main/java/com/ada/proj/dto/BanDetailResponse.java
package com.ada.proj.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BanDetailResponse {

    private Long banId;

    private String targetUuid;
    private String targetNickname;
    private String targetProfileImage;

    private String adminUuid;
    private String adminNickname;

    private String reason;
    private LocalDateTime bannedAt;
    private LocalDateTime expiresAt;
    private Boolean active;
}