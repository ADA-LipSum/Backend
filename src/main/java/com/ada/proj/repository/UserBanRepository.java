// src/main/java/com/ada/proj/repository/UserBanRepository.java
package com.ada.proj.repository;

import com.ada.proj.entity.User;
import com.ada.proj.entity.UserBan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserBanRepository extends JpaRepository<UserBan, Long> {

    Optional<UserBan> findByTargetUserAndActiveIsTrue(User targetUser);

    List<UserBan> findByExpiresAtBefore(LocalDateTime now); // 정상 작동
}