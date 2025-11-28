// src/main/java/com/ada/proj/dto/ReportCreateRequest.java
package com.ada.proj.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportCreateRequest {

    private String targetUuid;  // 게시글/댓글/유저 대상 UUID
    private String reportType;  // POST / COMMENT / USER
    private String reason;      // 이유
}