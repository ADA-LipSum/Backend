package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 선생님 자가 회원가입 요청 DTO
 */
@Data
@Schema(name = "TeacherSignupRequest", description = "선생님 회원가입 요청 바디")
public class TeacherSignupRequest {

    @NotBlank
    @Size(max = 50)
    @Schema(description = "선생님 식별자(내부적으로 adminId로 저장)", example = "tch-0001")
    private String teacherId; // 내부적으로 adminId 로 저장

    @NotBlank
    @Size(max = 10)
    @Schema(description = "실명", example = "홍길동")
    private String userRealname;

    @NotBlank
    @Size(max = 10)
    @Schema(description = "닉네임", example = "길동쌤")
    private String userNickname;

    // 로그인에 사용할 커스텀 계정
    @NotBlank
    @Size(min = 3, max = 50)
    @Schema(description = "로그인 ID", example = "teacher01")
    private String customId;

    @NotBlank
    @Size(min = 6, max = 255, message = "비밀번호는 6자 이상이어야 합니다")
    @Schema(description = "로그인 비밀번호(6자 이상)", example = "P@ssw0rd!")
    private String password;
}
