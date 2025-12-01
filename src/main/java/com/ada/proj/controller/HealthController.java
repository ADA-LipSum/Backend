package com.ada.proj.controller;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.HealthLogResponse;
import com.ada.proj.service.HealthLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@Tag(name = "서비스 상태", description = "헬스 체크 및 최근 로그 요약 제공 API")
public class HealthController {

    private final HealthLogService healthLogService;

    public HealthController(HealthLogService healthLogService) {
        this.healthLogService = healthLogService;
    }

    @GetMapping("api/health")
    @Operation(summary = "로그 기반 헬스체크: 최근 로그 요약 반환")
    public ResponseEntity<ApiResponse<HealthLogResponse>> health() {
        var summary = healthLogService.summarize(10);
        String status = summary.errorCount() > 0 ? "DEGRADED" : "UP";
        HealthLogResponse body = HealthLogResponse.builder()
                .status(status)
                .lastLogAt(summary.lastLogAt())
                .warnCount(summary.warnCount())
                .errorCount(summary.errorCount())
                .lastLines(summary.lastLines())
                .build();
        return ResponseEntity.ok(ApiResponse.success(body));
    }
}
