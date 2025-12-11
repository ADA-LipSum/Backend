package com.ada.proj.service;

import com.ada.proj.dto.PageResponse;
import com.ada.proj.dto.ReportCreateRequest;
import com.ada.proj.dto.ReportResponse;
import com.ada.proj.entity.Report;
import com.ada.proj.enums.Role;
import com.ada.proj.entity.User;
import com.ada.proj.enums.ReportStatus;
import com.ada.proj.enums.ReportType;
import com.ada.proj.repository.CommentRepository;
import com.ada.proj.repository.PostRepository;
import com.ada.proj.repository.ReportRepository;
import com.ada.proj.repository.UserRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserRepository userRepo;
    private final ReportRepository reportRepo;
    private final UserBanService userBanService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public ReportResponse createReport(ReportCreateRequest req) {
        User reporter = requireAuthenticatedUser();
        userBanService.checkUserBanned(reporter);

        String targetUuid = Objects.requireNonNull(req.getTargetUuid(), "targetUuid는 필수입니다.").trim();
        ReportType type = req.getReportType();
        String reason = req.getReason().trim();

        validateTarget(type, targetUuid, reporter);
        enforceDuplicatePolicy(reporter.getUuid(), targetUuid, type);

        Report draft = Report.builder()
                .reporterUuid(reporter.getUuid())
                .targetUuid(targetUuid)
                .reportType(type)
                .reason(reason)
                .status(ReportStatus.PENDING)
                .build();

        Report saved = reportRepo.save(Objects.requireNonNull(draft));

        return ReportResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> getReportList(int page, int size, String sort, ReportStatus status, ReportType type) {
        User requester = requireAuthenticatedUser();
        requireManagePrivilege(requester);

        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("page와 size는 0보다 커야 합니다.");
        }

        size = Math.min(size, 100);
        Sort sortSpec = Objects.requireNonNull(buildSort(sort));
        Pageable pageable = PageRequest.of(page, size, sortSpec);

        Specification<Report> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("reportType"), type));
        }

        Page<Report> result = reportRepo.findAll(spec, pageable);
        List<ReportResponse> content = result.getContent().stream()
                .map(ReportResponse::from)
                .toList();

        return new PageResponse<>(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                content
        );
    }

    @Transactional
    public ReportResponse updateStatus(Long reportId, ReportStatus nextStatus) {
        User requester = requireAuthenticatedUser();
        requireManagePrivilege(requester);

        if (reportId == null) {
            throw new IllegalArgumentException("reportId는 필수입니다.");
        }
        if (nextStatus == null) {
            throw new IllegalArgumentException("nextStatus는 필수입니다.");
        }

        Report report = reportRepo.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고 내역을 찾을 수 없습니다."));
        report.updateStatus(nextStatus, requester.getUuid());
        return ReportResponse.from(report);
    }

    private void validateTarget(ReportType type, String targetUuid, User reporter) {
        String safeTargetUuid = Objects.requireNonNull(targetUuid, "targetUuid는 필수입니다.");
        switch (type) {
            case USER -> {
                if (reporter.getUuid().equals(safeTargetUuid)) {
                    throw new IllegalArgumentException("자기 자신은 신고할 수 없습니다.");
                }
                userRepo.findByUuid(safeTargetUuid)
                        .orElseThrow(() -> new IllegalArgumentException("대상 유저를 찾을 수 없습니다."));
            }
            case POST ->
                postRepository.findById(safeTargetUuid)
                        .orElseThrow(() -> new IllegalArgumentException("대상 게시글을 찾을 수 없습니다."));
            case COMMENT -> {
                boolean exists = commentRepository.existsByCommentUuid(safeTargetUuid);
                if (!exists) {
                    throw new IllegalArgumentException("대상 댓글을 찾을 수 없습니다.");
                }
            }
        }
    }

    private void enforceDuplicatePolicy(String reporterUuid, String targetUuid, ReportType type) {
        boolean exists = reportRepo.existsByReporterUuidAndTargetUuidAndReportTypeAndStatusIn(
                reporterUuid,
                targetUuid,
                type,
                List.of(ReportStatus.PENDING)
        );
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 신고한 대상입니다.");
        }
    }

    private Sort buildSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] token = sort.split(",");
        String property = token[0].trim();
        Sort.Direction direction = token.length > 1 && "asc".equalsIgnoreCase(token[1].trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        if (property.isEmpty()) {
            property = "createdAt";
        }
        return Sort.by(direction, property);
    }

    private User requireAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("인증 정보가 없습니다.");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof String principalStr && "anonymousUser".equals(principalStr)) {
            throw new AccessDeniedException("인증되지 않은 요청입니다.");
        }
        String userUuid = auth.getName();
        return userRepo.findByUuid(userUuid)
                .orElseThrow(() -> new AccessDeniedException("사용자 정보를 찾을 수 없습니다."));
    }

    private void requireManagePrivilege(User requester) {
        if (requester.getRole() != Role.ADMIN && requester.getRole() != Role.TEACHER) {
            throw new AccessDeniedException("신고 내역을 조회할 권한이 없습니다.");
        }
    }
}
