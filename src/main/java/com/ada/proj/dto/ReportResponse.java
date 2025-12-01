package com.ada.proj.dto;

import com.ada.proj.entity.Report;
import com.ada.proj.enums.ReportStatus;
import com.ada.proj.enums.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportResponse {

    @Schema(description = "신고 ID", example = "42")
    private Long reportId;

    @Schema(description = "신고자 UUID", example = "9d13ec47-e585-4cf7-b6dd-7fd8c97d3a2e")
    private String reporterUuid;

    @Schema(description = "신고 대상 UUID", example = "6d13ec47-e585-4cf7-b6dd-7fd8c97d3a2e")
    private String targetUuid;

    @Schema(description = "신고 유형", example = "USER")
    private ReportType reportType;

    @Schema(description = "신고 사유", example = "욕설 및 비방")
    private String reason;

    @Schema(description = "신고 상태", example = "PENDING")
    private ReportStatus status;

    @Schema(description = "신고 생성 시각", example = "2025-12-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "처리 시각", nullable = true)
    private LocalDateTime resolvedAt;

    @Schema(description = "처리자 UUID", nullable = true)
    private String resolvedBy;

    public static ReportResponse from(Report report) {
        return ReportResponse.builder()
                .reportId(report.getId())
                .reporterUuid(report.getReporterUuid())
                .targetUuid(report.getTargetUuid())
                .reportType(report.getReportType())
                .reason(report.getReason())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .resolvedBy(report.getResolvedBy())
                .build();
    }
}
