// src/main/java/com/ada/proj/dto/ReportListResponse.java
package com.ada.proj.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReportListResponse {

    private Long reportId;

    private String reporterUuid;
    private String reporterNickname;

    private String targetUuid;
    private String targetNickname;

    private String reportType;       // POST / COMMENT / USER
    private String reason;
    private LocalDateTime reportedAt;
}