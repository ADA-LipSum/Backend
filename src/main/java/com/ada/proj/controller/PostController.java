package com.ada.proj.controller;

import java.util.Objects;

import com.ada.proj.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.service.PostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
@Tag(name = "게시물", description = "게시물 CRUD, 좋아요 등 게시판 기능 API")
public class PostController {

    private final PostService postService;
    private final com.ada.proj.service.CommentService commentService; // 댓글 조회 REST 경로 제공

    @PostMapping(path = {"", "/"})
    @Operation(
            summary = "게시물 생성",
            description = "JSON body로 게시물을 생성합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<String>> create(
            @Valid @RequestBody PostCreateRequest data,
            Authentication authentication
    ) {
        PostCreateRequest payload = Objects.requireNonNull(data, "data");
        if (authentication != null) {
            payload.setWriterUuid(authentication.getName());
        }

        String uuid = postService.create(payload);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.success(uuid));
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "게시글 수정", description = "title, content, isDev, devTags 선택 수정", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> update(
            @Parameter(description = "게시글 UUID", example = "post-uuid-...")
            @PathVariable("uuid") String uuid,
            @RequestBody PostUpdateRequest req,
            Authentication authentication) {
        postService.update(requireUuid(uuid), requireUpdateRequest(req), authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // seq 기반 수정
    @PutMapping("/seq/{seq}")
    @Operation(summary = "게시글 수정 (seq)", description = "seq로 게시글 수정", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> updateBySeq(
            @Parameter(description = "게시글 seq", example = "123")
            @PathVariable("seq") Long seq,
            @RequestBody PostUpdateRequest req,
            Authentication authentication) {
        postService.updateBySeq(requireSeq(seq), requireUpdateRequest(req), authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "게시글 삭제", description = "선택한 게시글을 삭제", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "게시글 UUID", example = "post-uuid-...")
            @PathVariable("uuid") String uuid,
            Authentication authentication) {
        postService.delete(requireUuid(uuid), authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // seq 기반 삭제
    @DeleteMapping("/seq/{seq}")
    @Operation(summary = "게시글 삭제 (seq)", description = "seq로 게시글 삭제", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteBySeq(
            @Parameter(description = "게시글 seq", example = "123")
            @PathVariable("seq") Long seq,
            Authentication authentication) {
        postService.deleteBySeq(requireSeq(seq), authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "게시글 상세 조회", description = "게시글 상세 정보를 조회하고 조회수 증가")
    public ResponseEntity<ApiResponse<PostDetailResponse>> detail(
            @Parameter(description = "게시글 UUID", example = "post-uuid-...")
            @PathVariable("uuid") String uuid) {
        return ResponseEntity.ok(ApiResponse.success(postService.detail(requireUuid(uuid))));
    }

    // seq 기반 상세 조회
    @GetMapping("/seq/{seq}")
    @Operation(summary = "게시글 상세 조회 (seq)", description = "seq로 게시글 상세 조회 및 조회수 증가")
    public ResponseEntity<ApiResponse<PostDetailResponse>> detailBySeq(
            @Parameter(description = "게시글 seq", example = "123")
            @PathVariable("seq") Long seq) {
        return ResponseEntity.ok(ApiResponse.success(postService.detailBySeq(requireSeq(seq))));
    }

    @GetMapping
    @Operation(summary = "게시글 목록 조회", description = "page/size 기반 목록 조회")
    public ResponseEntity<ApiResponse<PageResponse<PostSummaryResponse>>> list(
            @Parameter(description = "조회할 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "한 페이지에 포함될 게시글 개수", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.list(page, size)));
    }

    @PostMapping("/{uuid}/like")
    @Operation(summary = "게시글 좋아요 토글", description = "PathVariable 기반 토글")
    public ResponseEntity<ApiResponse<Boolean>> toggleLike(
            @PathVariable("uuid") String uuid,
            Authentication auth
    ) {
        if (auth == null) {
            throw new SecurityException("로그인이 필요합니다.");
        }
        String principal = Objects.requireNonNull(auth.getName(), "principal");
        boolean liked = postService.toggleLike(principal, requireUuid(uuid));
        return ResponseEntity.ok(ApiResponse.success(liked));
    }

    @PostMapping("/seq/{seq}/like")
    @Operation(summary = "게시글 좋아요 토글 (seq)", description = "seq로 좋아요 토글", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Boolean>> toggleLikeBySeq(
            @Parameter(description = "게시글 seq", example = "123")
            @PathVariable("seq") Long seq,
            Authentication auth
    ) {
        if (auth == null) {
            throw new SecurityException("로그인이 필요합니다.");
        }
        String principal = Objects.requireNonNull(auth.getName(), "principal");
        boolean liked = postService.toggleLikeBySeq(principal, requireSeq(seq));
        return ResponseEntity.ok(ApiResponse.success(liked));
    }

    @DeleteMapping("/likes/{id}")
    @Operation(summary = "좋아요 삭제 (id)", description = "좋아요 id로 좋아요 삭제", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteLikeById(
            @Parameter(description = "좋아요 id", example = "1")
            @PathVariable("id") Long id,
            Authentication auth
    ) {
        if (auth == null) {
            throw new SecurityException("로그인이 필요합니다.");
        }
        String principal = Objects.requireNonNull(auth.getName(), "principal");
        postService.deleteLikeById(requireLikeId(id), principal);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{uuid}/comments")
    @Operation(summary = "게시글 댓글 조회", description = "게시글의 최상위 및 대댓글 포함 전체 댓글 반환")
    public ResponseEntity<ApiResponse<java.util.List<CommentResponse>>> comments(
            @PathVariable("uuid") String uuid) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getCommentsByPost(requireUuid(uuid))));
    }

    // seq 기반 댓글 조회
    @GetMapping("/seq/{seq}/comments")
    @Operation(summary = "게시글 댓글 조회 (seq)", description = "seq로 게시글의 댓글 전체 조회")
    public ResponseEntity<ApiResponse<java.util.List<CommentResponse>>> commentsBySeq(
            @PathVariable("seq") Long seq) {
        PostDetailResponse pd = postService.detailBySeq(requireSeq(seq));
        return ResponseEntity.ok(ApiResponse.success(commentService.getCommentsByPost(requireUuid(pd.getPostUuid()))));
    }

    private @NonNull
    String requireUuid(String uuid) {
        return Objects.requireNonNull(uuid, "uuid");
    }

    private @NonNull
    Long requireSeq(Long seq) {
        return Objects.requireNonNull(seq, "seq");
    }

    private @NonNull
    Long requireLikeId(Long id) {
        return Objects.requireNonNull(id, "id");
    }

    private @NonNull
    PostUpdateRequest requireUpdateRequest(PostUpdateRequest req) {
        return Objects.requireNonNull(req, "request");
    }
}
