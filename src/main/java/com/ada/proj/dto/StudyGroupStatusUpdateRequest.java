package com.ada.proj.dto;

import com.ada.proj.enums.GroupStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudyGroupStatusUpdateRequest {

    @NotNull
    @Schema(description = "변경할 상태", example = "OPEN")
    private GroupStatus status;
}
