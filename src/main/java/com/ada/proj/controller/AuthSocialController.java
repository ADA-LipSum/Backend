package com.ada.proj.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.entity.User;
import com.ada.proj.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth")
@Tag(name = "인증", description = "인증 상태 확인 관련 API")
public class AuthSocialController {

    private final UserRepository userRepository;

    public AuthSocialController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Operation(summary = "인증 상태 조회", description = "현재 인증 상태와 사용자 기본 정보를 반환합니다.")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of("authenticated", false)));
        }

        String userUuid = authentication.getName();
        User user = userRepository.findByUuid(userUuid).orElse(null);

        Map<String, Object> resp = new HashMap<>();
        resp.put("authenticated", true);
        resp.put("uuid", userUuid);
        resp.put("user", user == null ? null : Map.of(
                "realname", user.getUserRealname(),
                "nickname", user.getUserNickname(),
                "profileImage", user.getProfileImage()));

        return ResponseEntity.ok(ApiResponse.ok(resp));
    }
}
