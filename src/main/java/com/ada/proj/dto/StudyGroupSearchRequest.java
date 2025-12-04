package com.ada.proj.dto;

import com.ada.proj.enums.GroupStatus;
import com.ada.proj.enums.GroupVisibility;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class StudyGroupSearchRequest {

    @Schema(description = "키워드(그룹명/설명 LIKE)")
    private String keyword;
    @Schema(description = "태그 포함(부분일치, 쉼표구분)")
    private String techTags;
    @Schema(description = "상태 필터(Open/Closed)")
    private GroupStatus status;
    @Schema(description = "공개여부 필터(PUBLIC/PRIVATE)")
    private GroupVisibility visibility;
    @Schema(description = "페이지 (0부터)")
    private Integer page = 0;
    @Schema(description = "페이지 크기")
    private Integer size = 10;
}
