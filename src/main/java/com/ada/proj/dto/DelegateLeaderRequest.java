package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DelegateLeaderRequest {
    @NotBlank
    @Schema(description = "리더로 위임할 사용자 UUID", example = "user-uuid-1234")
    private String leaderUserUuid;
}