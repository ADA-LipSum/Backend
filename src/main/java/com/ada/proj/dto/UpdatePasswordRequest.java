package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "UpdatePasswordRequest", description = "커스텀 비밀번호 변경 요청")
public class UpdatePasswordRequest {

    @NotBlank
    @Schema(description = "현재 비밀번호", example = "OldP@ss1!")
    private String currentPassword;

    @NotBlank
    @Size(min = 6, max = 255, message = "비밀번호는 6자 이상이어야 합니다")
    @Schema(description = "새 비밀번호(6자 이상)", example = "NewP@ss2!")
    private String newPassword;
}
