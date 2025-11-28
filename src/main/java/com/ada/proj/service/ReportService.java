// src/main/java/com/ada/proj/service/ReportService.java
package com.ada.proj.service;

import com.ada.proj.dto.PageResponse;
import com.ada.proj.dto.ReportCreateRequest;
import com.ada.proj.dto.ReportListResponse;
import com.ada.proj.entity.User;
import com.ada.proj.entity.UserReport;
import com.ada.proj.repository.UserReportRepository;
import com.ada.proj.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserRepository userRepo;
    private final UserReportRepository reportRepo;

    /**
     * 신고 생성 — reporterUuid는 JWT에서 자동 추출
     */
    public void createReport(ReportCreateRequest req) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new RuntimeException("인증되지 않은 요청입니다.");
        }

        // JWT에서 reporterUuid 자동 추출
        String reporterUuid = auth.getName();

        User reporter = userRepo.findByUuid(reporterUuid)
                .orElseThrow(() -> new RuntimeException("신고자를 찾을 수 없습니다."));

        User target = userRepo.findByUuid(req.getTargetUuid())
                .orElseThrow(() -> new RuntimeException("대상 유저를 찾을 수 없습니다."));

        UserReport report = UserReport.builder()
                .reporter(reporter)
                .targetUser(target)
                .reason(req.getReason())
                .reportType(req.getReportType())
                .reportedAt(LocalDateTime.now())
                .build();

        reportRepo.save(report);
    }

    /**
     * 신고 목록 페이징 조회
     */
    public PageResponse<ReportListResponse> getReportList(int page, int size) {

        if (page < 0) page = 0;
        if (size <= 0) size = 20;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reportedAt"));

        Page<UserReport> result = reportRepo.findAll(pageable);

        List<ReportListResponse> content = result.getContent().stream()
                .map(r -> ReportListResponse.builder()
                        .reportId(r.getId())
                        .reporterUuid(r.getReporter().getUuid())
                        .reporterNickname(r.getReporter().getUserNickname())
                        .targetUuid(r.getTargetUser().getUuid())
                        .targetNickname(r.getTargetUser().getUserNickname())
                        .reportType(r.getReportType())
                        .reason(r.getReason())
                        .reportedAt(r.getReportedAt())
                        .build()
                )
                .collect(Collectors.toList());

        return new PageResponse<>(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                content
        );
    }
}