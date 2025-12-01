package com.ada.proj.dto;

import com.ada.proj.enums.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportStatusUpdateRequest {

    @NotNull
    @Schema(description = "변경할 신고 상태", example = "RESOLVED")
    private ReportStatus status;
}
