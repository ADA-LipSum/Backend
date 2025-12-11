package com.ada.proj.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.ada.proj.enums.TradeCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "TradeItemCreateRequest", description = "거래 아이템 생성 요청 바디")
public class TradeItemCreateRequest {
    @NotBlank
    @Schema(description = "아이템 이름(상품명)", example = "프리미엄 배지")
    private String name;

    @Schema(description = "아이템 설명(옵션)", example = "프로필에 표시되는 골드 배지")
    private String description;

    @NotNull
    @Min(1)
    @Schema(description = "포인트 기준 가격(최소 1)", example = "100")
    private Integer price; // 단가(포인트)

    @NotNull
    @Min(0)
    @Schema(description = "초기 재고 수량(0 이상)", example = "50")
    private Integer stock; // 초기 재고

    @Schema(description = "판매 활성화 여부(기본 true)", example = "true")
    private Boolean active; // 기본 true

    @NotNull
    @Schema(description = "카테고리(FOOD | TOOLS | ETC)", example = "FOOD" )
    private TradeCategory category; // FOOD/TOOLS/...

    @Schema(description = "대표 이미지 URL(옵션)", example = "https://example.com/item.png")
    private String imageUrl; // 대표 이미지 URL
}
