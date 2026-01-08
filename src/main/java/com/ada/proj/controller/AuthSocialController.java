package com.ada.proj.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.entity.SocialAccount;
import com.ada.proj.entity.User;
import com.ada.proj.repository.SocialAccountRepository;
import com.ada.proj.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth")
@Tag(name = "소셜 계정 연동", description = "소셜 계정 연동 및 상태 확인 관련 API")
public class AuthSocialController {

    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;

    public AuthSocialController(SocialAccountRepository socialAccountRepository, UserRepository userRepository) {
        this.socialAccountRepository = socialAccountRepository;
        this.userRepository = userRepository;
    }

    @Operation(summary = "인증 상태 조회", description = "현재 인증 상태와 사용자 정보, 연동된 소셜 계정 목록을 반환합니다.")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of("authenticated", false)));
        }

        String userUuid = authentication.getName();
        List<SocialAccount> accounts = socialAccountRepository.findByUserUuid(userUuid);
        List<String> providers = accounts.stream().map(SocialAccount::getProvider).collect(Collectors.toList());

        Map<String, Object> links = new HashMap<>();

        Optional<SocialAccount> discordOpt = accounts.stream().filter(a -> "discord".equalsIgnoreCase(a.getProvider())).findFirst();
        boolean discordConnected = discordOpt.isPresent();
        String discordLink = null;
        if (discordConnected) {
            SocialAccount da = discordOpt.get();
            if (da.getProviderProfileUrl() != null) {
                discordLink = da.getProviderProfileUrl();
            } else if (da.getProviderId() != null) {
                discordLink = "https://discord.com/users/" + da.getProviderId();
            }
        }
        links.put("discord", discordLink);
        links.put("discordConnected", discordConnected);

        Optional<SocialAccount> linkedInOpt = accounts.stream()
                .filter(a -> "linked_in".equalsIgnoreCase(a.getProvider()) || "linkedin".equalsIgnoreCase(a.getProvider()))
                .findFirst();
        boolean linkedinConnected = linkedInOpt.isPresent();
        String linkedinLink = null;
        if (linkedinConnected) {
            SocialAccount la = linkedInOpt.get();
            if (la.getProviderProfileUrl() != null) {
                linkedinLink = la.getProviderProfileUrl();
            } else if (la.getProviderLogin() != null) {
                linkedinLink = "https://www.linkedin.com/in/" + la.getProviderLogin();
            }
        }
        links.put("linked_in", linkedinLink);
        links.put("linkedinConnected", linkedinConnected);

        User user = userRepository.findByUuid(userUuid).orElse(null);

        Map<String, Object> resp = new HashMap<>();
        resp.put("authenticated", true);
        resp.put("uuid", userUuid);
        resp.put("user", user == null ? null : Map.of(
                "realname", user.getUserRealname(),
                "nickname", user.getUserNickname(),
                "profileImage", user.getProfileImage()));
        resp.put("providers", providers);
        resp.put("links", links);

        return ResponseEntity.ok(ApiResponse.ok(resp));
    }
}
