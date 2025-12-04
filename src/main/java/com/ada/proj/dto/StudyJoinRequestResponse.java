package com.ada.proj.dto;

import com.ada.proj.enums.JoinRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class StudyJoinRequestResponse {

    @Schema(description = "사용자 UUID")
    private String userUuid;
    @Schema(description = "요청 상태")
    private JoinRequestStatus status;
    @Schema(description = "요청 시각")
    private Instant requestedAt;
    @Schema(description = "결정 시각")
    private Instant decidedAt;
}
