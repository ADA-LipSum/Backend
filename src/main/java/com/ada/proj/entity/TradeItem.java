package com.ada.proj.entity;

import java.time.Instant;

import com.ada.proj.enums.TradeCategory;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trade_item",
        indexes = {
                @Index(name = "idx_trade_item_uuid", columnList = "item_uuid"),
                @Index(name = "idx_trade_item_active", columnList = "active")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_trade_item_uuid", columnNames = {"item_uuid"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_uuid", length = 36, nullable = false, unique = true)
    private String itemUuid;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "price", nullable = false)
    private Integer price; // 포인트 가격(단가)

    @Column(name = "stock", nullable = false)
    private Integer stock; // 재고 수량

    @Column(name = "active", nullable = false)
    private Boolean active; // 판매 여부

        @Enumerated(EnumType.STRING)
        @Column(name = "category", length = 20, nullable = false)
        private TradeCategory category; // 카테고리

        @Column(name = "image_url", length = 500)
        private String imageUrl; // 대표 이미지 URL

    @Column(name = "created_by", length = 36)
    private String createdBy; // 생성자 uuid

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
