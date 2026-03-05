package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "CreateCustomLoginRequest", description = "사용자 커스텀 로그인 생성 요청")
public class CreateCustomLoginRequest {

    @NotBlank
    @Schema(description = "커스텀 로그인 ID", example = "user123")
    private String customId;

    @NotBlank
    @Size(min = 6, max = 255, message = "비밀번호는 6자 이상이어야 합니다")
    @Schema(description = "커스텀 로그인 비밀번호(6자 이상)", example = "P@ssw0rd!")
    private String password;
}
