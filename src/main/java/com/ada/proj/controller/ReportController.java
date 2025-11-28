// src/main/java/com/ada/proj/controller/ReportController.java
package com.ada.proj.controller;

import com.ada.proj.dto.PageResponse;
import com.ada.proj.dto.ReportCreateRequest;
import com.ada.proj.dto.ReportListResponse;
import com.ada.proj.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
@Tag(name = "Report", description = "유저 신고 API")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    // ======================================================
    // 신고 생성 (모든 인증된 유저)
    // ======================================================
    @Operation(
            summary = "신고 생성",
            description = """
                    특정 유저를 신고합니다.

                    reporterUuid는 JWT에서 자동 추출되어 request body에 포함하지 않습니다.

                    요청 예시:
                    {
                        "targetUuid": "UUID-OF-TARGET",
                        "reportType": "USER",
                        "reason": "욕설 / 비방 등"
                    }
                    """
    )
    @PostMapping
    public String report(@RequestBody ReportCreateRequest req) {
        reportService.createReport(req);
        return "신고가 접수되었습니다.";
    }

    // ======================================================
    // 신고 목록 조회 — 관리자/교사용
    // ======================================================
    @Operation(
            summary = "신고 목록 조회",
            description = """
                    신고 내역을 페이징으로 조회합니다.
                    ADMIN, TEACHER 계정만 접근 가능합니다.
                    """
    )
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping
    public PageResponse<ReportListResponse> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reportService.getReportList(page, size);
    }
}