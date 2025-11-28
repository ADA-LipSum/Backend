package com.ada.proj.service;

import com.ada.proj.entity.User;
import com.ada.proj.entity.UserBan;
import com.ada.proj.exception.BannedUserException;
import com.ada.proj.repository.UserBanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserBanService {

    private final UserBanRepository banRepo;

    /**
     * 유저가 제재 상태인지 확인
     * - active=true 이면 차단
     * - expiresAt(LocalDateTime)이 현재 시간보다 이전이면 자동 해제(active=false)
     */
    @Transactional
    public void checkUserBanned(User user) {

        UserBan ban = banRepo.findByTargetUserAndActiveIsTrue(user).orElse(null);

        // 제재 없음 → 정상
        if (ban == null) return;

        // 만료 확인
        if (ban.getExpiresAt() != null &&
                ban.getExpiresAt().isBefore(LocalDateTime.now())) {

            // 만료 → active=false 처리
            ban.setActive(false);
            banRepo.save(ban);
            return;
        }

        // 아직 만료되지 않은 active 제재 상태 → 차단
        throw new BannedUserException("해당 유저는 제재 상태로 인해 활동이 제한됩니다.");
    }
}