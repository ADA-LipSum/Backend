package com.ada.proj.controller;

import java.io.IOException;
import java.util.Objects;

import com.ada.proj.dto.*;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ada.proj.service.FileStorageService;
import com.ada.proj.service.FileStorageService.StoredFile;
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
@Tag(name = "게시물", description = "게시물 CRUD, 파일 업로드, 좋아요 등 게시판 기능 API")
public class PostController {

    private final PostService postService;
    private final FileStorageService fileStorageService;
    private final com.ada.proj.service.CommentService commentService; // 댓글 조회 REST 경로 제공

    // 파일 포함 생성
    @PostMapping(path = {"", "/", "/multipart"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "게시물 생성(파일 포함)",
            description = "@RequestPart('data') JSON에 title, content(contentMd 호환), isDev, devTags 포함 가능. 이미지/영상 파일 동시 업로드 지원.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            encoding = {
                @Encoding(name = "data", contentType = MediaType.APPLICATION_JSON_VALUE)}
    ))
    public ResponseEntity<ApiResponse<String>> createWithFiles(
            @Parameter(name = "data", description = "게시물 본문 JSON (title, content, isDev, devTags). images/videos는 서버가 파일로 자동 설정합니다.", required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PostCreateRequest.class),
                            examples = @ExampleObject(value = "{\n  \"title\": \"제목\",\n  \"content\": \"본문\",\n  \"isDev\": true,\n  \"devTags\": \"spring\"\n}")))
            @Valid @RequestPart("data") PostCreateRequest data,
            @Parameter(name = "files", description = "이미지/영상 혼합 업로드 (단일 배열)",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary")))
            @RequestPart(value = "files", required = false) MultipartFile[] files,
            @Parameter(name = "imageFiles", description = "이미지 파일 배열(하위호환)",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary")))
            @RequestPart(value = "imageFiles", required = false) MultipartFile[] imageFiles,
            @Parameter(name = "videoFiles", description = "영상 파일 배열(하위호환)",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary")))
            @RequestPart(value = "videoFiles", required = false) MultipartFile[] videoFiles,
            Authentication authentication
    ) throws IOException {
        PostCreateRequest payload = Objects.requireNonNull(data, "data");
        String uploaderUuid = authentication == null ? null : authentication.getName();
        if (authentication != null) {
            payload.setWriterUuid(authentication.getName());
        }

        StringBuilder md = new StringBuilder();
        if (payload.getContent() != null) {
            md.append(payload.getContent());
        }

        // 단일 files 배열도 허용(이미지/영상 자동 판별). 하위호환: imageFiles, videoFiles 유지
        appendMixedFiles(payload, files, md, uploaderUuid);
        appendImages(payload, imageFiles, md, uploaderUuid);
        appendVideos(payload, videoFiles, md, uploaderUuid);

        payload.setContent(md.toString());
        String uuid = postService.create(payload);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.success(uuid));
    }

    private void appendMixedFiles(PostCreateRequest data, MultipartFile[] files, StringBuilder md, String uploaderUuid) throws IOException {
        if (files == null) {
            return;
        }
        String firstImg = null;
        String firstVid = null;
        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) {
                continue;
            }
            StoredFile saved = fileStorageService.storeAuto(f, uploaderUuid);

            boolean isVideo = saved.contentType() != null && saved.contentType().startsWith("video/");
            boolean isImage = saved.contentType() != null && saved.contentType().startsWith("image/");

            if (isVideo) {
                if (firstVid == null) {
                    firstVid = saved.url();
                }
                md.append("\n\n<video controls src=\"")
                        .append(saved.url())
                        .append("\" style=\"max-width:100%\"></video>\n");
            } else {
                if (firstImg == null && isImage) {
                    firstImg = saved.url();
                }
                md.append("\n\n![image](").append(saved.url()).append(")\n");
            }
        }
        if (firstImg != null && (data.getImages() == null || data.getImages().isBlank())) {
            data.setImages(firstImg);
        }
        if (firstVid != null && (data.getVideos() == null || data.getVideos().isBlank())) {
            data.setVideos(firstVid);
        }
    }

    private void appendImages(PostCreateRequest data, MultipartFile[] imageFiles, StringBuilder md, String uploaderUuid) throws IOException {
        if (imageFiles == null) {
            return;
        }
        String firstImg = null;
        for (MultipartFile f : imageFiles) {
            if (f == null || f.isEmpty()) {
                continue;
            }
            StoredFile saved = fileStorageService.storeImage(f, uploaderUuid);
            if (firstImg == null) {
                firstImg = saved.url();
            }
            md.append("\n\n![image](").append(saved.url()).append(")\n");
        }
        if (firstImg != null && (data.getImages() == null || data.getImages().isBlank())) {
            data.setImages(firstImg);
        }
    }

    private void appendVideos(PostCreateRequest data, MultipartFile[] videoFiles, StringBuilder md, String uploaderUuid) throws IOException {
        if (videoFiles == null) {
            return;
        }
        String firstVid = null;
        for (MultipartFile f : videoFiles) {
            if (f == null || f.isEmpty()) {
                continue;
            }
            StoredFile saved = fileStorageService.storeVideo(f, uploaderUuid);
            if (firstVid == null) {
                firstVid = saved.url();
            }
            md.append("\n\n<video controls src=\"")
                    .append(saved.url())
                    .append("\" style=\"max-width:100%\"></video>\n");
        }
        if (firstVid != null && (data.getVideos() == null || data.getVideos().isBlank())) {
            data.setVideos(firstVid);
        }
    }

    @Deprecated
    @Hidden
    @PostMapping("/update")
    @Operation(summary = "[Deprecated] 수정", description = "PUT /api/posts/{uuid} 사용", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> legacyUpdate(
            @Parameter(description = "게시글 UUID", example = "post-uuid-...")
            @RequestParam("uuid") String uuid,
            @RequestBody PostUpdateRequest req,
            Authentication authentication) {
        postService.update(requireUuid(uuid), requireUpdateRequest(req), authentication);
        return ResponseEntity.ok(ApiResponse.success());
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

    @Deprecated
    @Hidden
    @PostMapping("/delete")
    @Operation(summary = "[Deprecated] 삭제", description = "DELETE /api/posts/{uuid} 사용", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> legacyDelete(
            @Parameter(description = "게시글 UUID", example = "post-uuid-...")
            @RequestParam("uuid") String uuid,
            Authentication authentication) {
        postService.delete(requireUuid(uuid), authentication);
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

    @Deprecated
    @Hidden
    @GetMapping("/view")
    @Operation(summary = "[Deprecated] 게시글 상세", description = "GET /api/posts/{uuid} 사용")
    public ResponseEntity<ApiResponse<PostDetailResponse>> legacyDetail(
            @Parameter(description = "게시글 UUID", example = "post-uuid-...")
            @RequestParam("uuid") String uuid) {
        return ResponseEntity.ok(ApiResponse.success(postService.detail(requireUuid(uuid))));
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

    @Deprecated
    @Hidden
    @GetMapping("/list")
    @Operation(summary = "[Deprecated] 게시글 목록", description = "GET /api/posts 사용")
    public ApiResponse<PageResponse<PostSummaryResponse>> legacyList(
            @Parameter(description = "조회할 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "한 페이지에 포함될 게시글 개수", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(postService.list(page, size));
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

    @Deprecated
    @Hidden
    @PostMapping("/like")
    @Operation(summary = "[Deprecated] 좋아요 토글", description = "POST /api/posts/{uuid}/like 사용")
    public ApiResponse<Boolean> legacyToggleLike(
            @RequestParam String uuid,
            Authentication auth
    ) {
        if (auth == null) {
            throw new SecurityException("로그인이 필요합니다.");
        }
        String principal = Objects.requireNonNull(auth.getName(), "principal");
        boolean liked = postService.toggleLike(principal, requireUuid(uuid));
        return ApiResponse.success(liked);
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
