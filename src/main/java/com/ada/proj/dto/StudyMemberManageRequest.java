package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StudyMemberManageRequest {
    @NotBlank
    @Schema(description = "대상 사용자 UUID", example = "user-uuid-1234")
    private String userUuid;
}
