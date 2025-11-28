// src/main/java/com/ada/proj/dto/BanResponse.java
package com.ada.proj.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BanResponse {

    private Long banId;
    private String targetUuid;
    private String adminUuid;
    private String reason;
    private LocalDateTime bannedAt;
    private LocalDateTime expiresAt;
    private Boolean active;
}