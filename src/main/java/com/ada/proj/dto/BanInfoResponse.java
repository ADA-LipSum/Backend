package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BanInfoResponse {

    @Schema(description = "제재 ID")
    private Long banId;

    @Schema(description = "제재 대상 유저 UUID")
    private String targetUuid;

    @Schema(description = "제재 대상 닉네임")
    private String targetNickname;

    @Schema(description = "제재 부여자 UUID")
    private String adminUuid;

    @Schema(description = "제재 부여자 닉네임")
    private String adminNickname;

    @Schema(description = "제재 사유")
    private String reason;

    @Schema(description = "현재 활성 상태")
    private boolean active;

    @Schema(description = "제재 시작 시각 (UTC)")
    private LocalDateTime startsAtUtc;

    @Schema(description = "제재 시작 시각 (KST)")
    private LocalDateTime startsAtKst;

    @Schema(description = "제재 만료 시각 (UTC)")
    private LocalDateTime expiresAtUtc;

    @Schema(description = "제재 만료 시각 (KST)")
    private LocalDateTime expiresAtKst;

    @Schema(description = "남은 시간(초). 영구 제재면 null")
    private Long remainingSeconds;
}
