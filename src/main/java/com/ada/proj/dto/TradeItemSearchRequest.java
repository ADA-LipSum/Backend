//C:\Users\russe\Documents\GitHub\Ada\Back\src\main\java\com\ada\proj\dto\TradeItemSearchRequest.java
package com.ada.proj.dto;

import com.ada.proj.enums.TradeCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "TradeItemSearchRequest", description = "아이템 검색/필터 요청 바디")
public class TradeItemSearchRequest {

    @Schema(description = "검색어(이름/설명 LIKE, 옵션)", example = "배지")
    private String keyword;

    @Schema(description = "카테고리 필터(FOOD|TOOLS|ETC, 옵션)", example = "TOOLS")
    private TradeCategory category;

    @Schema(description = "최소 가격(포인트, 옵션)", example = "50")
    private Integer minPrice;

    @Schema(description = "최대 가격(포인트, 옵션)", example = "200")
    private Integer maxPrice;

    @Schema(description = "판매 활성 여부 필터(기본 true)", example = "true")
    private Boolean active;

    @Schema(description = "페이지(0부터, 기본 0)", example = "0")
    private Integer page;

    @Schema(description = "페이지 크기(기본 20)", example = "20")
    private Integer size;

    @Schema(description = "정렬 필드(createdAt|price|name, 기본 createdAt)", example = "price")
    private String sort;

    @Schema(description = "정렬 방향(asc|desc, 기본 desc)", example = "desc")
    private String dir;
}