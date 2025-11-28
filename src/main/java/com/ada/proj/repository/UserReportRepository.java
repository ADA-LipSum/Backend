// src/main/java/com/ada/proj/repository/UserReportRepository.java
package com.ada.proj.repository;

import com.ada.proj.entity.UserReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserReportRepository extends JpaRepository<UserReport, Long> {
}