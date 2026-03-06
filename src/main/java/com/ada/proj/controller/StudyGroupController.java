package com.ada.proj.controller;

import com.ada.proj.dto.*;
import com.ada.proj.service.StudyGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/studies")
@Tag(name = "스터디 그룹", description = "스터디 그룹 생성/검색/가입/탈퇴/상태 변경 API")
public class StudyGroupController {

    private final StudyGroupService studyGroupService;

    @PostMapping("/groups")
    @Operation(summary = "그룹 생성", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody StudyGroupCreateRequest req,
            Authentication authentication) {
        String ownerUuid = authentication != null ? authentication.getName() : null;
        if (ownerUuid == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("UNAUTHORIZED", "인증이 필요합니다."));
        }
        String uuid = studyGroupService.create(java.util.Objects.requireNonNull(req), java.util.Objects.requireNonNull(ownerUuid));
        return ResponseEntity.status(201).body(ApiResponse.success(uuid));
    }

    @GetMapping("/groups/{uuid}")
    @Operation(summary = "그룹 상세 조회(공개 그룹은 누구나, 비공개는 멤버/방장만)")
    public ResponseEntity<ApiResponse<StudyGroupResponse>> detail(@PathVariable("uuid") String uuid) {
        return ResponseEntity.ok(ApiResponse.success(studyGroupService.getDetail(java.util.Objects.requireNonNull(uuid))));
    }

    @GetMapping("/groups")
    @Operation(summary = "그룹 검색", description = "기본적으로 공개 그룹만 검색. 인증 시 visibility 필터 사용 가능.")
    public ResponseEntity<ApiResponse<PageResponse<StudyGroupResponse>>> search(@Valid StudyGroupSearchRequest req) {
        return ResponseEntity.ok(ApiResponse.success(studyGroupService.search(java.util.Objects.requireNonNull(req))));
    }

    @GetMapping("/groups/{uuid}/members")
    @Operation(summary = "그룹 멤버 목록", description = "PUBLIC은 모두 조회 가능, PRIVATE은 멤버/방장/관리자만")
    public ResponseEntity<ApiResponse<java.util.List<StudyGroupMemberResponse>>> members(@PathVariable("uuid") String uuid) {
        return ResponseEntity.ok(ApiResponse.success(studyGroupService.listMembers(java.util.Objects.requireNonNull(uuid))));
    }

    @PostMapping("/groups/{uuid}/join")
    @Operation(summary = "그룹 가입", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> join(@PathVariable("uuid") String uuid, Authentication authentication) {
        String userUuid = authentication != null ? authentication.getName() : null;
        if (userUuid == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("UNAUTHORIZED", "인증이 필요합니다."));
        }
        studyGroupService.join(java.util.Objects.requireNonNull(uuid), java.util.Objects.requireNonNull(userUuid)); // 참가요청 생성
        return ResponseEntity.ok(ApiResponse.successMessage("참가요청이 등록되었습니다."));
    }

    @DeleteMapping("/groups/{uuid}/leave")
    @Operation(summary = "그룹 탈퇴", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> leave(@PathVariable("uuid") String uuid, Authentication authentication) {
        String userUuid = authentication != null ? authentication.getName() : null;
        if (userUuid == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("UNAUTHORIZED", "인증이 필요합니다."));
        }
        studyGroupService.leave(java.util.Objects.requireNonNull(uuid), java.util.Objects.requireNonNull(userUuid));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/groups/{uuid}/status")
    @Operation(summary = "그룹 상태 변경(OPEN/CLOSED)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable("uuid") String uuid,
            @Valid @RequestBody StudyGroupStatusUpdateRequest req) {
        studyGroupService.updateStatus(java.util.Objects.requireNonNull(uuid), java.util.Objects.requireNonNull(req));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/groups/{uuid}")
    @Operation(summary = "리더 위임", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> delegateLeader(@PathVariable("uuid") String uuid,
                                                            @Valid @RequestBody DelegateLeaderRequest req) {
        StudyMemberManageRequest manageReq = new StudyMemberManageRequest();
        manageReq.setUserUuid(req.getLeaderUserUuid());
        studyGroupService.delegateLeader(java.util.Objects.requireNonNull(uuid), java.util.Objects.requireNonNull(manageReq));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/groups/{uuid}/kick")
    @Operation(summary = "멤버 강제탈퇴", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> kick(@PathVariable("uuid") String uuid,
            @Valid @RequestBody StudyMemberManageRequest req) {
        studyGroupService.kickMember(java.util.Objects.requireNonNull(uuid), java.util.Objects.requireNonNull(req));
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 참가요청 목록(보류중)
    @GetMapping("/groups/{uuid}/requests")
    @Operation(summary = "참가요청 목록(보류중)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<java.util.List<StudyJoinRequestResponse>>> listRequests(@PathVariable("uuid") String uuid) {
        return ResponseEntity.ok(ApiResponse.success(studyGroupService.listPendingRequests(java.util.Objects.requireNonNull(uuid))));
    }

    // 참가요청 승인
    @PostMapping("/groups/{uuid}/requests/{userUuid}/approve")
    @Operation(summary = "참가요청 승인", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable("uuid") String uuid, @PathVariable String userUuid) {
        studyGroupService.approveRequest(java.util.Objects.requireNonNull(uuid), java.util.Objects.requireNonNull(userUuid));
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 참가요청 거절
    @PostMapping("/groups/{uuid}/requests/{userUuid}/reject")
    @Operation(summary = "참가요청 거절", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> reject(@PathVariable("uuid") String uuid, @PathVariable String userUuid) {
        studyGroupService.rejectRequest(java.util.Objects.requireNonNull(uuid), java.util.Objects.requireNonNull(userUuid));
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 내 참가요청 취소
    @DeleteMapping("/groups/{uuid}/requests/my")
    @Operation(summary = "내 참가요청 취소", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> cancelMy(@PathVariable("uuid") String uuid, Authentication authentication) {
        String userUuid = authentication != null ? authentication.getName() : null;
        if (userUuid == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("UNAUTHORIZED", "인증이 필요합니다."));
        }
        studyGroupService.cancelMyRequest(java.util.Objects.requireNonNull(uuid), java.util.Objects.requireNonNull(userUuid));
        return ResponseEntity.ok(ApiResponse.success());
    }
}
