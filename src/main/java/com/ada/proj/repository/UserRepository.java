package com.ada.proj.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ada.proj.entity.Role;
import com.ada.proj.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    @Cacheable(cacheNames = "users", key = "#uuid")
    Optional<User> findByUuid(String uuid);

    @Cacheable(cacheNames = "users", key = "'admin:' + #adminId")
    Optional<User> findByAdminId(String adminId);

    @Cacheable(cacheNames = "users", key = "'custom:' + #customId")
    Optional<User> findByCustomId(String customId);

    boolean existsByCustomId(String customId);

    @Query("select u from User u where (:role is null or u.role = :role) and (:query is null or u.userRealname like concat('%', :query, '%') or u.userNickname like concat('%', :query, '%')) order by u.createdAt desc, u.seq desc")
    List<User> search(@Param("role") Role role, @Param("query") String query);

    boolean existsByRole(Role role);
}
