package com.ada.proj.dto;

import com.ada.proj.enums.StudyMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class StudyGroupMemberResponse {
    @Schema(description = "사용자 UUID")
    private String userUuid;
    @Schema(description = "역할")
    private StudyMemberRole role;
    @Schema(description = "가입일")
    private Instant joinedAt;
}
