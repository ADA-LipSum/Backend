package com.ada.proj.controller;

import com.ada.proj.dto.*;
import com.ada.proj.enums.Role;
import com.ada.proj.entity.User;
import com.ada.proj.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping
@Tag(name = "회원/프로필", description = "회원 정보, 프로필, 커스텀 로그인 등을 관리하는 API")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    @Operation(summary = "유저 목록 조회(관리자)")
    public ResponseEntity<ApiResponse<List<User>>> list(
            @Parameter(description = "역할 필터", example = "TEACHER")
            @RequestParam(required = false) Role role,
            @Parameter(description = "검색어(이름/닉네임 포함)", example = "길동")
            @RequestParam(required = false, name = "q") String query
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.listUsers(role, query)));
    }

    @PatchMapping("/users/{uuid}/role")
    @Operation(summary = "권한 변경(관리자)")
    public ResponseEntity<ApiResponse<Void>> updateRole(
            @Parameter(description = "대상 사용자 UUID", example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
            @PathVariable String uuid,
            @Valid @RequestBody UpdateRoleRequest req) {
        userService.updateRole(uuid, req.getRole());
        return ResponseEntity.ok(ApiResponse.okMessage("role updated"));
    }

    @PatchMapping("/users/{uuid}/use-nickname")
    @Operation(summary = "닉네임으로 이름 표시 여부 토글")
    public ResponseEntity<ApiResponse<Void>> toggleUseNickname(
            @Parameter(description = "대상 사용자 UUID", example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
            @PathVariable String uuid) {
        userService.toggleUseNickname(uuid);
        return ResponseEntity.ok(ApiResponse.okMessage("toggled"));
    }

    @PatchMapping("/users/{uuid}/profile")
    @Operation(summary = "프로필 수정")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @Parameter(description = "대상 사용자 UUID", example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
            @PathVariable String uuid,
            @Valid @RequestBody UpdateProfileRequest req) {
        userService.updateProfile(uuid, req);
        return ResponseEntity.ok(ApiResponse.okMessage("profile updated"));
    }

    @PatchMapping("/users/{uuid}/custom/password")
    @Operation(summary = "커스텀 비밀번호 변경")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Parameter(description = "대상 사용자 UUID", example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
            @PathVariable String uuid,
            @Valid @RequestBody UpdatePasswordRequest req,
            Authentication auth) {
        userService.changeCustomPassword(uuid, req, auth);
        return ResponseEntity.ok(ApiResponse.okMessage("password updated"));
    }

    @PostMapping("/users/{uuid}/custom")
    @Operation(summary = "커스텀 ID/PW 생성(최초 1회)")
    public ResponseEntity<ApiResponse<Void>> createCustom(
            @Parameter(description = "대상 사용자 UUID", example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
            @PathVariable String uuid,
            @Valid @RequestBody CreateCustomLoginRequest req,
            Authentication auth) {
        userService.createCustomLogin(uuid, req, auth);
        return ResponseEntity.ok(ApiResponse.okMessage("custom login created"));
    }

    @GetMapping("/users/{uuid}")
    @Operation(summary = "유저 정보 조회")
    public ResponseEntity<ApiResponse<UserProfileResponse>> get(
            @Parameter(description = "대상 사용자 UUID", example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
            @PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserProfile(uuid)));
    }

    @PostMapping(path = "/users/{uuid}/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "프로필 이미지 업로드", description = "이미지 파일을 업로드하고 URL을 DB에 저장 후 프로필을 반환합니다.")
    public ResponseEntity<ApiResponse<UserProfileResponse>> uploadProfileImage(
            @PathVariable String uuid,
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestPart("file") MultipartFile file,
            Authentication auth
    ) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(userService.uploadProfileImage(uuid, file, auth)));
    }

    @PostMapping(path = "/users/{uuid}/profile/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "프로필 배너 업로드", description = "배너 이미지를 업로드하고 URL을 DB에 저장 후 프로필을 반환합니다.")
    public ResponseEntity<ApiResponse<UserProfileResponse>> uploadProfileBanner(
            @PathVariable String uuid,
            @Parameter(description = "업로드할 배너 이미지 파일", required = true)
            @RequestPart("file") MultipartFile file,
            Authentication auth
    ) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(userService.uploadProfileBanner(uuid, file, auth)));
    }
}
