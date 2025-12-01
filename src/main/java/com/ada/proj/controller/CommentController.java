package com.ada.proj.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.ada.proj.dto.CommentCreateRequest;
import com.ada.proj.dto.CommentResponse;
import com.ada.proj.dto.CommentUpdateRequest;
import com.ada.proj.dto.ApiResponse;
import com.ada.proj.service.CommentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "댓글", description = "댓글 작성/조회/수정/삭제 및 좋아요·고정 기능 API")
public class CommentController {

    private final CommentService commentService;

    /**
     * 댓글 작성
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    @Operation(
            summary = "댓글 작성",
            description = "게시물에 댓글을 작성합니다. parentId가 존재하면 대댓글로 등록됩니다."
    )
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @RequestBody @Valid CommentCreateRequest request
    ) {
        CommentCreateRequest payload = Objects.requireNonNull(request, "request");
        return ResponseEntity.ok(ApiResponse.success(commentService.createComment(payload)));
    }

    /**
     * 댓글 목록 조회
     */
    @GetMapping("/{postId}")
    @Operation(
            summary = "댓글 목록 조회",
            description = "해당 게시물(postId)의 모든 최상위 댓글과 대댓글(children)을 조회합니다."
    )
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @Parameter(description = "게시물 UUID") @PathVariable String postId
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getCommentsByPost(requireUuid(postId))));
    }

    /**
     * 댓글 수정
     */
    @PutMapping("/{commentId}")
    @Operation(
            summary = "댓글 수정",
            description = "로그인한 사용자가 자신의 댓글을 수정합니다. 본인이 작성한 댓글만 수정할 수 있습니다."
    )
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest req
    ) {
        Long id = requireCommentId(commentId);
        CommentUpdateRequest payload = Objects.requireNonNull(req, "request");
        return ResponseEntity.ok(ApiResponse.success(commentService.updateComment(id, payload)));
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/{commentId}")
    @Operation(
            summary = "댓글 삭제",
            description = "로그인한 사용자가 자신의 댓글을 삭제합니다. 본인이 작성한 댓글만 삭제할 수 있습니다."
    )
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(requireCommentId(commentId));
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 댓글 좋아요
     */
    @PostMapping("/{commentId}/like")
    @Operation(
            summary = "댓글 좋아요",
            description = "댓글 좋아요를 추가하거나 취소합니다. 같은 사용자가 다시 누르면 취소되는 토글 방식입니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleLike(
            @PathVariable Long commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentService.toggleLike(requireCommentId(commentId))));
    }

    /**
     * 댓글 고정 토글
     */
    @PostMapping("/{commentId}/fixed")
    @Operation(
            summary = "댓글 고정/해제",
            description = "게시글 작성자만 특정 댓글을 고정 또는 해제할 수 있습니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleFixed(
            @PathVariable Long commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentService.toggleFixed(requireCommentId(commentId))));
    }

    private @NonNull
    String requireUuid(String postId) {
        return Objects.requireNonNull(postId, "postId");
    }

    private @NonNull
    Long requireCommentId(Long commentId) {
        return Objects.requireNonNull(commentId, "commentId");
    }
}
