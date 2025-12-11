package com.ada.proj.dto;

import com.ada.proj.enums.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportCreateRequest {

    @NotBlank
    @Pattern(
            regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "UUID 형식이어야 합니다"
    )
    @Schema(description = "신고 대상 UUID", example = "6d13ec47-e585-4cf7-b6dd-7fd8c97d3a2e")
    private String targetUuid;

    @NotNull
    @Schema(description = "신고 유형", example = "USER")
    private ReportType reportType;

    @NotBlank
    @Size(min = 3, max = 255)
    @Schema(description = "신고 사유", example = "욕설 및 비방")
    private String reason;
}
