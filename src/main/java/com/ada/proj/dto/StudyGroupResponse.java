package com.ada.proj.dto;

import com.ada.proj.enums.GroupStatus;
import com.ada.proj.enums.GroupVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class StudyGroupResponse {

    @Schema(description = "그룹 UUID")
    private String groupUuid;
    @Schema(description = "그룹명")
    private String name;
    @Schema(description = "설명")
    private String description;
    @Schema(description = "기술 태그(쉼표구분)")
    private String techTags;
    @Schema(description = "공개 여부")
    private GroupVisibility visibility;
    @Schema(description = "모집 상태")
    private GroupStatus status;
    @Schema(description = "정원")
    private Integer capacity;
    @Schema(description = "방장 UUID")
    private String ownerUuid;
    @Schema(description = "현재 인원수")
    private Long memberCount;
    @Schema(description = "생성일")
    private Instant createdAt;
    @Schema(description = "수정일")
    private Instant updatedAt;
    @Schema(description = "요청자가 멤버인지 여부")
    private Boolean isMember;
    @Schema(description = "요청자의 그룹 내 역할 (LEADER/MEMBER)")
    private String myRole;
}
