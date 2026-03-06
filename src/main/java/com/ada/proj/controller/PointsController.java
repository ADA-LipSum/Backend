package com.ada.proj.controller;

import com.ada.proj.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.PageResponse;
import com.ada.proj.dto.PointsBalanceResponse;
import com.ada.proj.dto.PointsDeductRequest;
import com.ada.proj.dto.PointsGrantRequest;
import com.ada.proj.dto.PointsTransactionResponse;
import com.ada.proj.dto.PointsUseRequest;
import com.ada.proj.entity.UserPoints;
import com.ada.proj.service.PointsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
@Validated
@Tag(name = "포인트", description = "포인트 지급/차감/사용 및 잔액 조회 API")
public class PointsController {

    private final PointsService pointsService;
    private final UserService userService;

    // 현재 잔액 조회: 본인 또는 관리자만 가능
    @GetMapping("/balance/{userUuid}")
    @Operation(summary = "포인트 잔액 조회", description = "본인 또는 ADMIN이 특정 사용자의 현재 포인트 잔액을 조회합니다.")
        public ApiResponse<PointsBalanceResponse> getBalance(
            @Parameter(description = "대상 사용자 UUID", example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
            @PathVariable String userUuid, Authentication auth) {
        ensurePointViewPermission(auth, userUuid);
        int balance = pointsService.getBalance(userUuid);
        return ApiResponse.success(new PointsBalanceResponse(userUuid, balance));
    }

    // 잔액 조회 (쿼리 파라미터 버전)
    @GetMapping("/balance")
    @Operation(summary = "포인트 잔액 조회(쿼리)", description = "본인 또는 ADMIN이 특정 사용자의 현재 포인트 잔액을 조회합니다. 예: /api/points/balance?userUuid=...")
        public ApiResponse<PointsBalanceResponse> getBalanceQuery(
            @Parameter(description = "대상 사용자 UUID", example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
            @RequestParam String userUuid, Authentication auth) {
        return getBalance(userUuid, auth);
    }

    // 포인트 지급: 로그인 사용자 본인 UUID 자동 사용
    @PostMapping("/adjustments")
    @Operation(summary = "포인트 지급(지정 사용자)", description = "요청 본문으로 전달된 userUuid에 대해 포인트를 지급합니다. ADMIN 권한 불필요.")
    public ApiResponse<PointsTransactionResponse> grant(@Valid @RequestBody PointsGrantRequest req) {
        UserPoints tx = pointsService.grantPoints(req.getUserUuid(), req.getPoints(), req.getDescription(), req.getRefRuleId());
        return ApiResponse.success(PointsTransactionResponse.from(tx));
    }

    // 포인트 차감: 로그인 사용자 본인 UUID 자동 사용
    @PostMapping("/adjustments")
    @Operation(summary = "포인트 차감(지정 사용자)", description = "요청 본문으로 전달된 userUuid에 대해 포인트를 차감합니다. ADMIN 권한 불필요.")
    public ApiResponse<PointsTransactionResponse> deduct(@Valid @RequestBody PointsDeductRequest req) {
        UserPoints tx = pointsService.deductPoints(req.getUserUuid(), req.getPoints(), req.getDescription(), req.getRefRuleId());
        return ApiResponse.success(PointsTransactionResponse.from(tx));
    }

    // 포인트 사용: 본인 전용
    @PostMapping("/adjustments")
    @Operation(summary = "포인트 사용", description = "본인 또는 ADMIN 대행으로 포인트를 사용 처리합니다. 본문에 대상 사용자 UUID를 명시합니다.")
    public ApiResponse<PointsTransactionResponse> use(@Valid @RequestBody PointsUseRequest req, Authentication auth) {
        ensureSelfOrAdmin(auth, req.getUserUuid()); // 관리자 대행 사용 허용
        UserPoints tx = pointsService.usePoints(req.getUserUuid(), req.getPoints(), req.getUsedFor(), req.getMetadata(), req.getDescription());
        return ApiResponse.success(PointsTransactionResponse.from(tx));
    }

    // 거래내역 조회 (페이징)
    @GetMapping("/transactions")
    @Operation(summary = "포인트 거래내역 조회", description = "특정 사용자(userUuid)의 포인트 거래내역을 최신순으로 페이징하여 조회합니다.")
        public ApiResponse<PageResponse<PointsTransactionResponse>> getTransactions(
            @Parameter(description = "대상 사용자 UUID", example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
            @RequestParam String userUuid,
            @Parameter(description = "페이지(0부터)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        ensureSelfOrAdmin(auth, userUuid);
        var pageResult = pointsService.getTransactions(userUuid, page, size).map(PointsTransactionResponse::from);
        PageResponse<PointsTransactionResponse> body = new PageResponse<>(
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.getContent());
        return ApiResponse.success(body);
    }

    private void ensureSelfOrAdmin(Authentication auth, String userUuid) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !auth.getName().equals(userUuid)) {
            throw new SecurityException("Forbidden");
        }
    }

    private void ensurePointViewPermission(Authentication auth, String targetUuid) {
        if (auth == null) {
            throw new SecurityException("Unauthenticated");
        }

        String currentUuid = auth.getName();

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean isTeacher = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));

        boolean isStudent = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        // 관리자 → 모두 허용
        if (isAdmin) return;

        // 학생 → 자기 자신만
        if (isStudent) {
            if (currentUuid.equals(targetUuid)) return;
            throw new SecurityException("Forbidden: 학생 계정은 자신의 포인트만 조회할 수 있습니다.");
        }

        // 선생 → 학생 포인트만 가능
        if (isTeacher) {
            boolean targetIsStudent = userService.isStudent(targetUuid);
            if (targetIsStudent) return;
            throw new SecurityException("Forbidden: 선생님 계정은 학생 포인트만 조회할 수 있습니다.");
        }

        throw new SecurityException("Forbidden");
    }
}
