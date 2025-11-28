// src/main/java/com/ada/proj/controller/BanController.java
package com.ada.proj.controller;

import com.ada.proj.dto.BanCreateRequest;
import com.ada.proj.service.BanService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ban")
@RequiredArgsConstructor
@Tag(
        name = "Ban",
        description = """
                유저 제재 관리 API
                
                - ADMIN, TEACHER 계정만 사용 가능
                - JWT 인증 필요
                - user_ban 테이블 = 제재 + 로그 기록
                """
)
// OpenApiConfig 의 이름과 반드시 동일해야 함 (bearerAuth)
@SecurityRequirement(name = "bearerAuth")
public class BanController {

    private final BanService banService;

    @Operation(
            summary = "유저 제재 부여",
            description = """
                    targetUuid 유저에게 제재를 부여합니다.

                    expiresAt 예시:
                    - 2025-12-01T12:00:00Z   ← Swagger 기본값 (UTC)
                    - 2025-12-01T12:00:00    ← KST

                    서버는 모든 날짜를 KST(UTC+9) 기준으로 변환해 저장합니다.
                    """
    )
    @ApiResponse(responseCode = "200", description = "제재 적용 성공")
    @ApiResponse(responseCode = "401", description = "JWT 인증 실패")
    @ApiResponse(responseCode = "403", description = "권한 없음")
    @PostMapping("/give")
    public String giveBan(@RequestBody BanCreateRequest req) {
        banService.giveBan(req);
        return "제재가 적용되었습니다.";
    }

    @Operation(summary = "유저 제재 해제 (UUID)")
    @PostMapping("/release/{userUuid}")
    public String releaseBan(@PathVariable String userUuid) {
        banService.releaseBan(userUuid);
        return "제재가 해제되었습니다.";
    }

    @Operation(summary = "제재 해제 (banId)")
    @PostMapping("/release/manual/{banId}")
    public String releaseManual(@PathVariable Long banId) {
        banService.releaseBanManual(banId);
        return "success";
    }
}