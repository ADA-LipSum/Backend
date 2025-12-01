package com.ada.proj.dto;

import com.ada.proj.enums.DurationUnit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BanRequest {

    @NotBlank
    @Schema(description = "제재 대상 유저 UUID", example = "6d13ec47-e585-4cf7-b6dd-7fd8c97d3a2e")
    private String targetUuid;

    @NotBlank
    @Size(max = 500)
    @Schema(description = "제재 사유", example = "욕설 및 비방", maxLength = 500)
    private String reason;

    @Positive
    @Schema(description = "제재 기간 값", example = "3")
    private long durationValue;

    @NotNull
    @Schema(description = "제재 기간 단위", example = "DAYS")
    private DurationUnit durationUnit;
}
