// src/main/java/com/ada/proj/repository/UserBanRepository.java
package com.ada.proj.repository;

import com.ada.proj.entity.User;
import com.ada.proj.entity.UserBan;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBanRepository extends JpaRepository<UserBan, Long> {

    Optional<UserBan> findByTargetUserAndActiveIsTrue(User targetUser);

    Page<UserBan> findByActive(boolean active, Pageable pageable);

    List<UserBan> findByTargetUserOrderByBannedAtDesc(User targetUser);

    List<UserBan> findByTargetUserAndActiveOrderByBannedAtDesc(User targetUser, boolean active);

    List<UserBan> findByExpiresAtBefore(LocalDateTime now); // 정상 작동
}
