// src/main/java/com/ada/proj/dto/ReportDetailResponse.java
package com.ada.proj.dto;

import com.ada.proj.enums.ReportType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportDetailResponse {

    private Long reportId;

    private String reporterUuid;
    private String reporterNickname;
    private String reporterProfileImage;

    private String targetUuid;
    private String targetNickname;
    private String targetProfileImage;

    private ReportType reportType;
    private String reason;
    private LocalDateTime reportedAt;
}
