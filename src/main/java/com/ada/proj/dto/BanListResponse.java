// src/main/java/com/ada/proj/dto/BanListResponse.java
package com.ada.proj.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BanListResponse {

    private Long banId;

    private String targetUuid;
    private String targetNickname;

    private String adminUuid;       // 제재한 관리자
    private String adminNickname;

    private String reason;
    private LocalDateTime bannedAt;
    private LocalDateTime expiresAt;
    private Boolean active;
}