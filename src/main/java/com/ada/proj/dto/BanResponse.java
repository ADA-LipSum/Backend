// src/main/java/com/ada/proj/dto/BanResponse.java
package com.ada.proj.dto;

import com.ada.proj.enums.DurationUnit;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BanResponse {

    @Schema(description = "제재 대상 유저 UUID")
    private String targetUuid;

    @Schema(description = "제재 사유")
    private String reason;

    @Schema(description = "제재 기간 값")
    private long durationValue;

    @Schema(description = "제재 기간 단위")
    private DurationUnit durationUnit;

    @Schema(description = "제재 시작 시각 (KST)")
    private LocalDateTime startsAtKst;

    @Schema(description = "제재 만료 시각 (KST)")
    private LocalDateTime expiresAtKst;

    @Schema(description = "제재 만료 시각 (UTC)")
    private LocalDateTime expiresAtUtc;
}
