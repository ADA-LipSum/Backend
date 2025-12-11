package com.ada.proj.dto;

import java.time.Instant;

import com.ada.proj.enums.PointChangeType;
import com.ada.proj.entity.UserPoints;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PointsTransactionResponse {
    @Schema(example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
    private String pointsUuid;
    private String userUuid;
    private PointChangeType changeType;
    private int points;
    private int balanceAfter;
    private String description;
    private Long refRuleId;
    private String refEventUuid;
    private Instant createdAt;

    public static PointsTransactionResponse from(UserPoints up) {
        PointsTransactionResponse r = new PointsTransactionResponse();
        r.setPointsUuid(up.getPointsUuid());
        r.setUserUuid(up.getUserUuid());
        r.setChangeType(up.getChangeType());
        r.setPoints(up.getPoints());
        r.setBalanceAfter(up.getBalanceAfter());
        r.setDescription(up.getDescription());
        r.setRefRuleId(up.getRefRuleId());
        r.setRefEventUuid(up.getRefEventUuid());
        r.setCreatedAt(up.getCreatedAt());
        return r;
    }
}
