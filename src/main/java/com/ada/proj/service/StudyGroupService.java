package com.ada.proj.service;

import com.ada.proj.dto.PageResponse;
import com.ada.proj.dto.StudyGroupCreateRequest;
import com.ada.proj.dto.StudyGroupMemberResponse;
import com.ada.proj.dto.StudyGroupResponse;
import com.ada.proj.dto.StudyGroupSearchRequest;
import com.ada.proj.dto.StudyGroupStatusUpdateRequest;
import com.ada.proj.dto.StudyMemberManageRequest;
import com.ada.proj.dto.StudyJoinRequestResponse;
import com.ada.proj.entity.StudyGroup;
import com.ada.proj.entity.StudyGroupMember;
import com.ada.proj.entity.StudyGroupJoinRequest;
import com.ada.proj.enums.GroupStatus;
import com.ada.proj.enums.GroupVisibility;
import com.ada.proj.enums.StudyMemberRole;
import com.ada.proj.enums.JoinRequestStatus;
import com.ada.proj.repository.StudyGroupMemberRepository;
import com.ada.proj.repository.StudyGroupRepository;
import com.ada.proj.repository.StudyGroupJoinRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudyGroupService {

    private final StudyGroupRepository studyGroupRepository;
    private final StudyGroupMemberRepository memberRepository;
    private final StudyGroupJoinRequestRepository joinRequestRepository;

    private boolean hasAdminRole(Authentication auth) {
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (ga != null && "ROLE_ADMIN".equalsIgnoreCase(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private String currentUserUuidOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return null;

    }

    private Specification<StudyGroup> buildSpec(StudyGroupSearchRequest req, boolean forcePublicOnly) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            if (forcePublicOnly) {
                predicates.getExpressions().add(cb.equal(root.get("visibility"), GroupVisibility.PUBLIC));
            } else if (req.getVisibility() != null) {
                predicates.getExpressions().add(cb.equal(root.get("visibility"), req.getVisibility()));
            }

            if (req.getStatus() != null) {
                predicates.getExpressions().add(cb.equal(root.get("status"), req.getStatus()));
            }

            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                String like = "%" + req.getKeyword().toLowerCase(Locale.ROOT) + "%";
                predicates.getExpressions().add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("description")), like)
                ));
            }

            if (req.getTechTags() != null && !req.getTechTags().isBlank()) {
                // 간단히 부분일치. CSV 저장 구조 상 contains로 검색 처리
                String likeTag = "%" + req.getTechTags().toLowerCase(Locale.ROOT) + "%";
                predicates.getExpressions().add(cb.like(cb.lower(root.get("techTags")), likeTag));
            }

            return predicates;
        };
    }

    private StudyGroupResponse toResponse(StudyGroup g, String requesterUuid) {
        Long memberCount = memberRepository.countByGroup_GroupUuid(g.getGroupUuid());
        boolean isMember = false;
        String myRole = null;
        if (requesterUuid != null) {
            var mem = memberRepository.findByGroup_GroupUuidAndUserUuid(g.getGroupUuid(), requesterUuid).orElse(null);
            if (mem != null) {
                isMember = true;
                myRole = mem.getRole() != null ? mem.getRole().name() : null;
            }
        }

        return StudyGroupResponse.builder()
                .groupUuid(g.getGroupUuid())
                .name(g.getName())
                .description(g.getDescription())
                .techTags(g.getTechTags())
                .visibility(g.getVisibility())
                .status(g.getStatus())
                .capacity(g.getCapacity())
                .ownerUuid(g.getOwnerUuid())
                .memberCount(memberCount)
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .isMember(isMember)
                .myRole(myRole)
                .build();
    }

    @Transactional
    public String create(@NonNull StudyGroupCreateRequest req, @NonNull String ownerUuid) {
        Objects.requireNonNull(req.getName(), "name");
        Objects.requireNonNull(req.getVisibility(), "visibility");
        Objects.requireNonNull(req.getCapacity(), "capacity");

        if (req.getCapacity() < 1) {
            throw new IllegalArgumentException("정원은 1 이상이어야 합니다.");
        }

        StudyGroup g = StudyGroup.builder()
                .name(req.getName())
                .description(req.getDescription())
                .techTags(req.getTechTags())
                .visibility(req.getVisibility())
                .status(GroupStatus.OPEN)
                .capacity(req.getCapacity())
                .ownerUuid(ownerUuid)
                .build();
        g = studyGroupRepository.save(java.util.Objects.requireNonNull(g));

        // 방장 멤버십 자동 추가
        StudyGroupMember leader = StudyGroupMember.builder()
                .group(g)
                .userUuid(ownerUuid)
                .role(StudyMemberRole.LEADER)
                .build();
        memberRepository.save(java.util.Objects.requireNonNull(leader));

        return g.getGroupUuid();
    }

    @Transactional(readOnly = true)
    public StudyGroupResponse getDetail(@NonNull String groupUuid) {
        StudyGroup g = studyGroupRepository.findByGroupUuid(groupUuid)
                .orElseThrow(() -> new EntityNotFoundException("StudyGroup not found: " + groupUuid));

        String requester = currentUserUuidOrNull();

        // PRIVATE 그룹은 멤버 또는 방장만 조회 가능
        if (g.getVisibility() == GroupVisibility.PRIVATE) {
            if (requester == null) {
                throw new EntityNotFoundException("StudyGroup not found: " + groupUuid);
            }
            boolean allowed = requester.equals(g.getOwnerUuid())
                    || memberRepository.existsByGroup_GroupUuidAndUserUuid(groupUuid, requester);
            if (!allowed) {
                throw new EntityNotFoundException("StudyGroup not found: " + groupUuid);
            }
        }

        return toResponse(g, requester);
    }

    @Transactional(readOnly = true)
    public PageResponse<StudyGroupResponse> search(@NonNull StudyGroupSearchRequest req) {
        int page = Optional.ofNullable(req.getPage()).orElse(0);
        int size = Optional.ofNullable(req.getSize()).orElse(10);
        if (size > 100) {
            size = 100;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean forcePublicOnly = (auth == null || !auth.isAuthenticated());

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<StudyGroup> result = studyGroupRepository.findAll(buildSpec(req, forcePublicOnly), pageable);
        String requester = currentUserUuidOrNull();

        List<StudyGroupResponse> list = result.map(g -> toResponse(g, requester)).getContent();
        return new PageResponse<>(result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(), list);
    }

    @Transactional
    public void join(@NonNull String groupUuid, @NonNull String userUuid) {
        StudyGroup g = studyGroupRepository.findByGroupUuid(groupUuid)
                .orElseThrow(() -> new EntityNotFoundException("StudyGroup not found: " + groupUuid));

        if (memberRepository.existsByGroup_GroupUuidAndUserUuid(groupUuid, userUuid)) {
            return; // 이미 멤버면 무시
        }
        // 이미 보류중 요청 있으면 재요청 방지
        if (joinRequestRepository.existsByGroup_GroupUuidAndUserUuidAndStatus(groupUuid, userUuid, JoinRequestStatus.PENDING)) {
            return;
        }
        // 요청 생성
        StudyGroupJoinRequest req = StudyGroupJoinRequest.builder()
                .group(g)
                .userUuid(userUuid)
                .status(JoinRequestStatus.PENDING)
                .build();
        joinRequestRepository.save(java.util.Objects.requireNonNull(req));
    }

    @Transactional
    public void leave(@NonNull String groupUuid, @NonNull String userUuid) {
        studyGroupRepository.findByGroupUuid(groupUuid)
                .orElseThrow(() -> new EntityNotFoundException("StudyGroup not found: " + groupUuid));

        StudyGroupMember m = memberRepository.findByGroup_GroupUuidAndUserUuid(groupUuid, userUuid)
                .orElseThrow(() -> new EntityNotFoundException("멤버가 아닙니다."));
        if (m.getRole() == StudyMemberRole.LEADER) {
            throw new IllegalStateException("리더는 탈퇴할 수 없습니다. 리더 위임 또는 그룹 상태 변경이 필요합니다.");
        }
        memberRepository.delete(m);
    }

    @Transactional
    public void updateStatus(@NonNull String groupUuid, @NonNull StudyGroupStatusUpdateRequest req) {
        StudyGroup g = studyGroupRepository.findByGroupUuid(groupUuid)
                .orElseThrow(() -> new EntityNotFoundException("StudyGroup not found: " + groupUuid));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String requester = auth != null ? auth.getName() : null;
        boolean isOwner = requester != null && requester.equals(g.getOwnerUuid());
        boolean isAdmin = hasAdminRole(auth);

        if (!isOwner && !isAdmin) {
            throw new SecurityException("상태 변경 권한이 없습니다.");
        }

        g.setStatus(Objects.requireNonNull(req.getStatus()));
    }

    @Transactional(readOnly = true)
    public List<StudyJoinRequestResponse> listPendingRequests(@NonNull String groupUuid) {
        StudyGroup g = studyGroupRepository.findByGroupUuid(groupUuid)
                .orElseThrow(() -> new EntityNotFoundException("StudyGroup not found: " + groupUuid));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String requester = auth != null ? auth.getName() : null;
        boolean isOwner = requester != null && requester.equals(g.getOwnerUuid());
        boolean isAdmin = hasAdminRole(auth);
        if (!isOwner && !isAdmin) {
            throw new SecurityException("요청 목록을 볼 권한이 없습니다.");
        }

        return joinRequestRepository.findAllByGroup_GroupUuidAndStatusOrderByRequestedAtDesc(groupUuid, JoinRequestStatus.PENDING)
                .stream()
                .map(r -> StudyJoinRequestResponse.builder()
                .userUuid(r.getUserUuid())
                .status(r.getStatus())
                .requestedAt(r.getRequestedAt())
                .decidedAt(r.getDecidedAt())
                .build())
                .toList();
    }

    @Transactional
    public void approveRequest(@NonNull String groupUuid, @NonNull String targetUserUuid) {
        StudyGroup g = studyGroupRepository.findByGroupUuid(groupUuid)
                .orElseThrow(() -> new EntityNotFoundException("StudyGroup not found: " + groupUuid));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String requester = auth != null ? auth.getName() : null;
        boolean isOwner = requester != null && requester.equals(g.getOwnerUuid());
        boolean isAdmin = hasAdminRole(auth);
        if (!isOwner && !isAdmin) {
            throw new SecurityException("승인 권한이 없습니다.");
        }

        // 정원/상태 체크
        if (g.getStatus() != GroupStatus.OPEN) {
            throw new IllegalStateException("모집중이 아닙니다.");
        }
        long count = memberRepository.countByGroup_GroupUuid(groupUuid);
        if (count >= g.getCapacity()) {
            throw new IllegalStateException("정원이 가득 찼습니다.");
        }

        // 요청 조회
        StudyGroupJoinRequest jr = joinRequestRepository.findByGroup_GroupUuidAndUserUuid(groupUuid, targetUserUuid)
                .orElseThrow(() -> new EntityNotFoundException("참가요청을 찾을 수 없습니다."));
        if (jr.getStatus() != JoinRequestStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }

        // 멤버 추가
        if (!memberRepository.existsByGroup_GroupUuidAndUserUuid(groupUuid, targetUserUuid)) {
            StudyGroupMember m = StudyGroupMember.builder()
                    .group(g)
                    .userUuid(targetUserUuid)
                    .role(StudyMemberRole.MEMBER)
                    .build();
            memberRepository.save(java.util.Objects.requireNonNull(m));
        }
        jr.setStatus(JoinRequestStatus.APPROVED);
        jr.setDecidedAt(java.time.Instant.now());
    }

    @Transactional
    public void rejectRequest(@NonNull String groupUuid, @NonNull String targetUserUuid) {
        StudyGroup g = studyGroupRepository.findByGroupUuid(groupUuid)
                .orElseThrow(() -> new EntityNotFoundException("StudyGroup not found: " + groupUuid));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String requester = auth != null ? auth.getName() : null;
        boolean isOwner = requester != null && requester.equals(g.getOwnerUuid());
        boolean isAdmin = hasAdminRole(auth);
        if (!isOwner && !isAdmin) {
            throw new SecurityException("거절 권한이 없습니다.");
        }

        StudyGroupJoinRequest jr = joinRequestRepository.findByGroup_GroupUuidAndUserUuid(groupUuid, targetUserUuid)
                .orElseThrow(() -> new EntityNotFoundException("참가요청을 찾을 수 없습니다."));
        if (jr.getStatus() != JoinRequestStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }
        jr.setStatus(JoinRequestStatus.REJECTED);
        jr.setDecidedAt(java.time.Instant.now());
    }

    @Transactional
    public void cancelMyRequest(@NonNull String groupUuid, @NonNull String userUuid) {
        StudyGroupJoinRequest jr = joinRequestRepository.findByGroup_GroupUuidAndUserUuid(groupUuid, userUuid)
                .orElseThrow(() -> new EntityNotFoundException("참가요청을 찾을 수 없습니다."));
        if (jr.getStatus() != JoinRequestStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }
        jr.setStatus(JoinRequestStatus.CANCELED);
        jr.setDecidedAt(java.time.Instant.now());
    }

    @Transactional(readOnly = true)
    public List<StudyGroupMemberResponse> listMembers(@NonNull String groupUuid) {
        StudyGroup g = studyGroupRepository.findByGroupUuid(groupUuid)
                .orElseThrow(() -> new EntityNotFoundException("StudyGroup not found: " + groupUuid));

        String requester = currentUserUuidOrNull();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = hasAdminRole(auth);
        boolean isOwner = requester != null && requester.equals(g.getOwnerUuid());
        boolean isMember = requester != null && memberRepository.existsByGroup_GroupUuidAndUserUuid(groupUuid, requester);

        if (g.getVisibility() == GroupVisibility.PRIVATE && !(isOwner || isAdmin || isMember)) {
            throw new EntityNotFoundException("StudyGroup not found: " + groupUuid);
        }

        return memberRepository.findAllByGroup_GroupUuid(groupUuid).stream()
                .map(m -> StudyGroupMemberResponse.builder()
                .userUuid(m.getUserUuid())
                .role(m.getRole())
                .joinedAt(m.getJoinedAt())
                .build())
                .toList();
    }

    @Transactional
    public void delegateLeader(@NonNull String groupUuid, @NonNull StudyMemberManageRequest req) {
        StudyGroup g = studyGroupRepository.findByGroupUuid(groupUuid)
                .orElseThrow(() -> new EntityNotFoundException("StudyGroup not found: " + groupUuid));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String requester = auth != null ? auth.getName() : null;
        boolean isOwner = requester != null && requester.equals(g.getOwnerUuid());
        boolean isAdmin = hasAdminRole(auth);
        if (!isOwner && !isAdmin) {
            throw new SecurityException("리더 위임 권한이 없습니다.");
        }

        String target = Objects.requireNonNull(req.getUserUuid(), "userUuid");
        StudyGroupMember targetMember = memberRepository.findByGroup_GroupUuidAndUserUuid(groupUuid, target)
                .orElseThrow(() -> new EntityNotFoundException("대상은 그룹 멤버가 아닙니다."));

        // 현재 리더(방장) 멤버 레코드
        StudyGroupMember currentLeader = memberRepository.findByGroup_GroupUuidAndUserUuid(groupUuid, g.getOwnerUuid())
                .orElse(null);
        if (currentLeader != null) {
            currentLeader.setRole(StudyMemberRole.MEMBER);
        }

        targetMember.setRole(StudyMemberRole.LEADER);
        g.setOwnerUuid(target);
    }

    @Transactional
    public void kickMember(@NonNull String groupUuid, @NonNull StudyMemberManageRequest req) {
        StudyGroup g = studyGroupRepository.findByGroupUuid(groupUuid)
                .orElseThrow(() -> new EntityNotFoundException("StudyGroup not found: " + groupUuid));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String requester = auth != null ? auth.getName() : null;
        boolean isOwner = requester != null && requester.equals(g.getOwnerUuid());
        boolean isAdmin = hasAdminRole(auth);
        if (!isOwner && !isAdmin) {
            throw new SecurityException("강제탈퇴 권한이 없습니다.");
        }

        String target = Objects.requireNonNull(req.getUserUuid(), "userUuid");
        StudyGroupMember targetMember = memberRepository.findByGroup_GroupUuidAndUserUuid(groupUuid, target)
                .orElseThrow(() -> new EntityNotFoundException("대상은 그룹 멤버가 아닙니다."));
        if (targetMember.getRole() == StudyMemberRole.LEADER) {
            throw new IllegalStateException("리더는 강제탈퇴할 수 없습니다. 먼저 리더 위임을 진행하세요.");
        }
        memberRepository.delete(targetMember);
    }
}
