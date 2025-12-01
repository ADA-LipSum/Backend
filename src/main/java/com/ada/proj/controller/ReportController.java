// src/main/java/com/ada/proj/controller/ReportController.java
package com.ada.proj.controller;

import com.ada.proj.dto.PageResponse;
import com.ada.proj.dto.ReportCreateRequest;
import com.ada.proj.dto.ReportResponse;
import com.ada.proj.dto.ReportStatusUpdateRequest;
import com.ada.proj.enums.ReportStatus;
import com.ada.proj.enums.ReportType;
import com.ada.proj.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
@Tag(name = "신고 관리", description = "유저 신고 생성 및 목록 조회 API")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class ReportController {

    private final ReportService reportService;

    @Operation(
            summary = "신고 생성",
            description = "신고자는 JWT 정보에서 자동으로 식별되며 요청 본문에는 대상/유형/사유만 담습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "신고 접수 완료",
                content = @Content(schema = @Schema(implementation = ReportResponse.class))),
        @ApiResponse(responseCode = "400", description = "검증 실패", content = @Content),
        @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse createReport(@Valid @RequestBody ReportCreateRequest req) {
        return reportService.createReport(req);
    }

    @Operation(
            summary = "신고 목록 조회",
            description = "신고 내역을 페이징/필터링/정렬하여 조회합니다. 관리자 또는 교사 권한만 접근 가능합니다.",
            parameters = {
                @Parameter(name = "page", description = "0부터 시작하는 페이지 번호"),
                @Parameter(name = "size", description = "페이지당 데이터 수 (최대 100)"),
                @Parameter(name = "sort", description = "정렬 기준 (예: createdAt,desc)", example = "createdAt,desc"),
                @Parameter(name = "status", description = "필터링할 신고 상태"),
                @Parameter(name = "reportType", description = "필터링할 신고 유형")
            }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
                content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터", content = @Content),
        @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping
    public PageResponse<ReportResponse> getReports(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(name = "reportType", required = false) ReportType type
    ) {
        return reportService.getReportList(page, size, sort, status, type);
    }

    @Operation(
            summary = "신고 상태 변경",
            description = "관리자 또는 교사가 신고 상태를 처리 상태로 변경합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "상태 변경 완료",
                content = @Content(schema = @Schema(implementation = ReportResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
        @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PatchMapping("/{reportId}/status")
    public ReportResponse updateReportStatus(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportStatusUpdateRequest request
    ) {
        return reportService.updateStatus(reportId, request.getStatus());
    }
}
