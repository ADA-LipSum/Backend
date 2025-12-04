package com.ada.proj.dto;

import com.ada.proj.enums.GroupVisibility;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudyGroupCreateRequest {

    @NotBlank
    @Schema(description = "그룹명", example = "스프링 스터디")
    private String name;

    @Schema(description = "그룹 설명")
    private String description;

    @Schema(description = "기술 태그(쉼표구분)", example = "spring,java,jpa")
    private String techTags;

    @NotNull
    @Schema(description = "공개 여부", example = "PUBLIC")
    private GroupVisibility visibility;

    @NotNull
    @Min(1)
    @Max(1000)
    @Schema(description = "최대 인원", example = "10")
    private Integer capacity;
}
