package com.ada.proj.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.ada.proj.enums.PointChangeType;
import com.ada.proj.entity.PointsEventLog;
import com.ada.proj.entity.PointsUsageHistory;
import com.ada.proj.entity.UserPoints;
import com.ada.proj.entity.UserPointsBalance;
import com.ada.proj.repository.PointsEventLogRepository;
import com.ada.proj.repository.PointsUsageHistoryRepository;
import com.ada.proj.repository.UserPointsBalanceRepository;
import com.ada.proj.repository.UserPointsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PointsService {

    private final UserPointsRepository userPointsRepository;
    private final UserPointsBalanceRepository balanceRepository;
    private final PointsEventLogRepository eventLogRepository;
    private final PointsUsageHistoryRepository usageHistoryRepository;

    @Transactional(readOnly = true)
    public int getBalance(String userUuid) {
        return balanceRepository.findByUserUuid(userUuid)
                .map(UserPointsBalance::getTotalPoints)
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public Page<UserPoints> getTransactions(String userUuid, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("seq")));
        return userPointsRepository.findByUserUuidOrderByCreatedAtDescSeqDesc(userUuid, pageable);
    }

    @Transactional
    public UserPoints grantPoints(String userUuid, int points, String description, Long refRuleId) {
        return applyChange(userUuid, PointChangeType.GAIN, points, description, refRuleId, null);
    }

    @Transactional
    public UserPoints deductPoints(String userUuid, int points, String description, Long refRuleId) {
        return applyChange(userUuid, PointChangeType.LOSS, points, description, refRuleId, null);
    }

    @Transactional
    public UserPoints usePoints(String userUuid, int points, String usedFor, String metadata, String description) {
        // Apply deduction first
        UserPoints tx = applyChange(userUuid, PointChangeType.USE, points, description, null, null);

        // Record usage detail
        PointsUsageHistory usage = PointsUsageHistory.builder()
                .usageUuid(UUID.randomUUID().toString())
                .userUuid(userUuid)
                .pointsUuid(tx.getPointsUuid())
                .usedFor(usedFor)
                .metadata(metadata)
                .build();
        usageHistoryRepository.save(usage);

        return tx;
    }

    @Transactional
    public UserPoints refund(String userUuid, String originalPointsUuid, int points, String description) {
        // Refund is treated as GAIN linked to original event.
        return applyChange(userUuid, PointChangeType.REFUND, points, description, null, originalPointsUuid);
    }

    @Transactional
    public UserPoints applyRule(String userUuid, String ruleCode, String description, int pointsByRule, PointChangeType type, Long ruleId) {
        // This helper can be used by higher-level rule service; here we just log event
        UserPoints tx = applyChange(userUuid, type, pointsByRule, description, ruleId, null);
        PointsEventLog log = PointsEventLog.builder()
                .eventUuid(UUID.randomUUID().toString())
                .userUuid(userUuid)
                .ruleId(ruleId)
                .pointsUuid(tx.getPointsUuid())
                .build();
        eventLogRepository.save(log);
        tx.setRefEventUuid(log.getEventUuid());
        return tx;
    }

    @Transactional
    protected UserPoints applyChange(String userUuid, PointChangeType type, int points, String description, Long refRuleId, String refEventUuid) {
        if (userUuid == null || userUuid.isBlank()) {
            throw new IllegalArgumentException("userUuid는 필수입니다.");
        }
        if (points <= 0) {
            throw new IllegalArgumentException("points는 1 이상이어야 합니다.");
        }

        // Lock balance row for update
        UserPointsBalance balance = balanceRepository.findByUserUuidForUpdate(userUuid)
                .orElseGet(() -> {
                    UserPointsBalance b = UserPointsBalance.builder()
                            .userUuid(userUuid)
                            .totalPoints(0)
                            .build();
                    return balanceRepository.save(b);
                });

        int delta = switch (type) {
            case GAIN, REFUND -> points;
            case LOSS, USE -> -points;
        };

        long newBalanceLong = (long) balance.getTotalPoints() + delta;
        if (newBalanceLong < 0) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
        int newBalance = (int) newBalanceLong;

        // Create transaction
        String pointsUuid = UUID.randomUUID().toString();
        UserPoints tx = UserPoints.builder()
                .pointsUuid(pointsUuid)
                .userUuid(userUuid)
                .changeType(type)
                .points(delta)
                .balanceAfter(newBalance)
                .description(description)
                .refRuleId(refRuleId)
                .refEventUuid(refEventUuid)
                .build();
        userPointsRepository.save(tx);

        // Update balance snapshot
        balance.setTotalPoints(newBalance);
        balanceRepository.save(balance);

        return tx;
    }
}
