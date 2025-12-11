package com.ada.proj.dto;

import com.ada.proj.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
@Schema(name = "CreateUserRequest", description = "관리자에 의한 사용자 생성 요청")
public class CreateUserRequest {
	@NotBlank
	@Schema(description = "관리자 발급 ID(내부 식별자)", example = "adm-0003")
	private String adminId; // internal admin identifier for the user

	@NotBlank
	@Size(max = 10)
	@Schema(description = "실명", example = "김학생")
	private String userRealname;

	@NotBlank
	@Size(max = 10)
	@Schema(description = "닉네임", example = "김코딩")
	private String userNickname;

	@Schema(description = "역할(기본 STUDENT)", example = "STUDENT")
	private Role role = Role.STUDENT;

	// optional: set initial custom login
	@Size(min = 3, max = 50)
	@Schema(description = "초기 커스텀 로그인 ID(옵션)", example = "student01")
	private String customId;

	@Size(min = 6, max = 255)
	@Schema(description = "초기 커스텀 로그인 비밀번호(옵션)", example = "P@ssw0rd!")
	private String password;

	// 프로필 확장 필드(옵션)
	@Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.png")
	private String profileImage;

	@Schema(description = "프로필 배너 URL", example = "https://example.com/banner.png")
	private String profileBanner;

	@Size(max = 255)
	@Schema(description = "자기소개", example = "안녕하세요, 백엔드 개발자입니다.")
	private String intro;

	@Schema(description = "기술 스택(배열)", example = "[\"React\", \"Spring\"]")
	private List<String> techStack;

	@Schema(description = "외부 링크 정보(JSON)")
	private ProfileLinksRequest links;
}
