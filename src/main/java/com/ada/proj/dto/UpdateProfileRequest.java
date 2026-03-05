package com.ada.proj.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "UpdateProfileRequest", description = "프로필 수정 요청 바디")
public class UpdateProfileRequest {

    @Size(max = 10)
    @Schema(description = "닉네임(최대 10자)", example = "길동쌤")
    private String nickname;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.png")
    private String profileImage;

    @Schema(description = "프로필 배너 URL", example = "https://example.com/banner.png")
    private String profileBanner;

    @Size(max = 255)
    @Schema(description = "자기소개", example = "안녕하세요, 백엔드 개발자입니다.")
    private String intro;

    @Schema(description = "기술 스택(배열)", example = "[\"React\", \"Spring\", \"MySQL\"]")
    private List<String> techStack;
}
