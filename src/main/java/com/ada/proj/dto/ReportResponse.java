// src/main/java/com/ada/proj/dto/ReportResponse.java
package com.ada.proj.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReportResponse {

    private Long reportId;
    private String reporterUuid;
    private String targetUuid;
    private String reportType;
    private String reason;
    private LocalDateTime reportedAt;
}