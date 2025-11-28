// src/main/java/com/ada/proj/dto/BanCreateRequest.java
package com.ada.proj.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BanCreateRequest {

    @Schema(description = "제재 대상 유저 UUID", example = "zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz")
    private String targetUuid;

    @Schema(description = "제재 사유", example = "욕설 및 비방")
    private String reason;

    @Schema(
            description = "제재 만료 시간 (null이면 영구 제재)",
            example = "2025-12-01T12:00:00"
    )
    private LocalDateTime expiresAt;  // String → LocalDateTime
}