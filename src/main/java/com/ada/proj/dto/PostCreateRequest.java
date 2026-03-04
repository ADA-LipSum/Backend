// src/main/java/com/ada/proj/dto/post/PostCreateRequest.java
package com.ada.proj.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PostCreateRequest {
    // 서버 자동 설정
    @Schema(description = "서버에서 자동 설정", accessMode = Schema.AccessMode.READ_ONLY, hidden = true)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String writerUuid;          // 작성자 UUID

    @NotBlank @Size(max = 20)
    @Schema(example = "게시물 제목")
    private String title;

    // 콘텐츠 원문(마크다운/HTML)
    @JsonAlias({"contentMd"})
    @Schema(example = "본문 내용입니다")
    private String content;
    @Schema(description = "대표 이미지 URL", accessMode = Schema.AccessMode.READ_ONLY, hidden = true)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String images;              // 이미지 URL
    @Schema(description = "대표 영상 URL", accessMode = Schema.AccessMode.READ_ONLY, hidden = true)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String videos;              // 영상 URL

    // 태그(프론트 분류)
    @Schema(description = "개발글 여부", example = "true")
    private Boolean isDev;
    @Schema(description = "개발 언어 CSV (예: Python,C)", example = "spring")
    private String devTags;
}