package com.ada.proj.dto;

import com.ada.proj.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "AdminCreateUserRequest", description = "관리자에 의한 사용자 생성 요청")
public class AdminCreateUserRequest {

    @NotBlank
    @Schema(description = "관리자 발급 ID(내부 식별자) - 유저 고정 아이디", example = "adm-0003")
    private String adminId;

    @NotBlank
    @Size(max = 10)
    @Schema(description = "실명", example = "김학생")
    private String userRealname;

    @Schema(description = "역할(기본 STUDENT)", example = "STUDENT")
    private Role role = Role.STUDENT;

    @Size(min = 6, max = 255, message = "비밀번호는 6자 이상이어야 합니다")
    @Schema(description = "초기 커스텀 로그인 비밀번호(옵션, 6자 이상)", example = "P@ssw0rd!")
    private String password;
}