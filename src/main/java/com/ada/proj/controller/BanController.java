// src/main/java/com/ada/proj/controller/BanController.java
package com.ada.proj.controller;

import com.ada.proj.dto.BanInfoResponse;
import com.ada.proj.dto.BanRequest;
import com.ada.proj.dto.BanResponse;
import com.ada.proj.dto.PageResponse;
import com.ada.proj.service.BanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "제재 관리", description = "유저 제재 관리 API (ADMIN/TEACHER 전용)")
@SecurityRequirement(name = "bearerAuth")
public class BanController {

    private final BanService banService;

    @Operation(
            summary = "제재 생성",
            description = "운영자가 특정 유저에게 기간 제재를 부여합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = BanRequest.class),
                            examples = @ExampleObject(
                                    value = """
                        {
                          \"targetUuid\": \"6d13ec47-e585-4cf7-b6dd-7fd8c97d3a2e\",
                          \"reason\": \"욕설 및 비방\",
                          \"durationValue\": 3,
                          \"durationUnit\": \"DAYS\"
                        }
                        """
                            )
                    )
            ),
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "제재 적용 성공",
                        content = @Content(
                                schema = @Schema(implementation = BanResponse.class),
                                examples = @ExampleObject(
                                        value = """
                            {
                              \"targetUuid\": \"6d13ec47-e585-4cf7-b6dd-7fd8c97d3a2e\",
                              \"reason\": \"욕설 및 비방\",
                              \"durationValue\": 3,
                              \"durationUnit\": \"DAYS\",
                              \"startsAtKst\": \"2025-12-01T10:00:00\",
                              \"expiresAtKst\": \"2025-12-04T10:00:00\",
                              \"expiresAtUtc\": \"2025-12-04T01:00:00\"
                            }
                            """
                                )
                        )
                ),
                @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                @ApiResponse(responseCode = "401", description = "JWT 인증 실패"),
                @ApiResponse(responseCode = "403", description = "권한 없음"),
                @ApiResponse(responseCode = "409", description = "이미 제재 중")
            }
    )
    @PostMapping("/bans")
    public ResponseEntity<BanResponse> createBan(@Valid @RequestBody BanRequest request) {
        BanResponse response = banService.createBan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "제재 목록 조회",
            description = "ADMIN/TEACHER가 제재 내역을 페이징으로 조회합니다.")
    @GetMapping("/bans")
    public PageResponse<BanInfoResponse> getBans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return banService.getBanList(activeOnly, page, size);
    }

    @Operation(
            summary = "내 제재 현황",
            description = "로그인한 사용자가 본인에게 적용된 활성 제재 정보를 확인합니다. 제재 중이 아니면 204를 반환합니다.")
    @GetMapping("/me/ban")
    public ResponseEntity<BanInfoResponse> getMyBanInfo() {
        BanInfoResponse info = banService.getMyActiveBan();
        if (info == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(info);
    }

    @Operation(
            summary = "특정 유저 제재 내역",
            description = "특정 유저의 제재 내역을 조회합니다. activeOnly=true이면 현재 진행 중인 제재만 반환합니다.")
    @GetMapping("/bans/users/{userUuid}")
    public List<BanInfoResponse> getBanHistory(
            @PathVariable String userUuid,
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return banService.getBanHistoryForUser(userUuid, activeOnly);
    }

    @Operation(summary = "유저 제재 해제 (UUID)")
    @PostMapping("/bans/{userUuid}/release")
    public String releaseBan(@PathVariable String userUuid) {
        banService.releaseBan(userUuid);
        return "제재가 해제되었습니다.";
    }

    @Operation(summary = "제재 해제 (banId)")
    @PostMapping("/bans/{banId}/release/manual")
    public String releaseManual(@PathVariable Long banId) {
        banService.releaseBanManual(banId);
        return "success";
    }
}
