package com.ada.proj.dto;

import java.time.Instant;

import com.ada.proj.entity.TradeItem;
import com.ada.proj.enums.TradeCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TradeItemResponse", description = "거래 아이템 응답")
public class TradeItemResponse {
    @Schema(description = "내부 식별자", example = "12")
    private Long id;

    @Schema(description = "아이템 UUID", example = "1b6c2a5f-0d7a-4d09-9c4a-1f2e3a4b5c6d")
    private String itemUuid;

    @Schema(description = "아이템 이름", example = "프리미엄 배지")
    private String name;

    @Schema(description = "아이템 설명", example = "프로필에 표시되는 골드 배지")
    private String description;

    @Schema(description = "단가(포인트)", example = "100")
    private Integer price;

    @Schema(description = "재고 수량", example = "50")
    private Integer stock;

    @Schema(description = "판매 활성 여부", example = "true")
    private Boolean active;

    @Schema(description = "카테고리", example = "TOOLS")
    private TradeCategory category;

    @Schema(description = "대표 이미지 URL", example = "https://example.com/item.png")
    private String imageUrl;

    @Schema(description = "생성 시각", example = "2025-11-16T01:53:26Z")
    private Instant createdAt;

    public static TradeItemResponse from(TradeItem t) {
        return TradeItemResponse.builder()
                .id(t.getId())
                .itemUuid(t.getItemUuid())
                .name(t.getName())
                .description(t.getDescription())
                .price(t.getPrice())
                .stock(t.getStock())
                .active(t.getActive())
                .category(t.getCategory())
                .imageUrl(t.getImageUrl())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
