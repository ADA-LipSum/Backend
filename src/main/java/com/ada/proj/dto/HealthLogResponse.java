package com.ada.proj.dto;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthLogResponse {

    @Schema(example = "UP")
    private String status;
    @Schema(description = "헬스 체크 처리에 소요된 시간(ms)", example = "5")
    private long pingMs;
    private Instant lastLogAt;
    private int warnCount;
    private int errorCount;
    private List<String> lastLines;
}
