package com.ada.proj.dto;

import com.ada.proj.enums.PointChangeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "포인트 조정 요청")
public class PointsAdjustRequest {

    @NotBlank
    @Schema(description = "대상 사용자 UUID", example = "user-uuid-1234")
    private String userUuid;

    @NotNull
    @Schema(description = "조정 유형 (GAIN/LOSS/USE)", example = "GAIN")
    private PointChangeType type;

    @Min(1)
    @Schema(description = "포인트 수량", example = "100")
    private int points;

    @Schema(description = "설명", example = "출석 보상")
    private String description;

    @Schema(description = "규칙 ID (옵션)")
    private Long refRuleId;

    @Schema(description = "사용처 (USE 타입 시)", example = "trade")
    private String usedFor;

    @Schema(description = "메타데이터 (USE 타입 시, JSON 문자열)")
    private String metadata;
}