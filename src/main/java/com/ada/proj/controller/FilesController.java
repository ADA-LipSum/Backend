package com.ada.proj.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.service.FileStorageService;
import com.ada.proj.service.FileStorageService.StoredFile;
import com.ada.proj.service.S3ObjectStorage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/files")
@Tag(name = "파일 업로드", description = "이미지/영상 파일을 업로드하고 URL을 반환하는 API")
public class FilesController {

    private final FileStorageService fileStorageService;
    private final S3ObjectStorage s3ObjectStorage;

    public FilesController(FileStorageService fileStorageService, S3ObjectStorage s3ObjectStorage) {
        this.fileStorageService = fileStorageService;
        this.s3ObjectStorage = s3ObjectStorage;
    }

    public enum UploadType {
        auto,
        image,
        video
    }

    public record S3ListItem(
            String key,
            Long sizeBytes,
            java.time.Instant lastModified,
            String folder,
            String storedName,
            String url,
            String originalName,
            String contentType
            ) {

    }

    @Operation(summary = "파일 업로드(통합)", description = "이미지/영상 파일을 여러 개 업로드하고 공개 URL 리스트를 반환합니다. type=auto(기본)은 확장자 기반으로 images/videos 자동 분류합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<StoredFile>>> upload(
            @Parameter(description = "업로드 타입 (auto/image/video)")
            @RequestParam(name = "type", required = false, defaultValue = "auto") UploadType type,
            @Parameter(description = "업로드할 파일 배열", required = true)
            @RequestPart("files") MultipartFile[] files,
            Authentication authentication
    ) throws Exception {
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body(ApiResponse.errorWithData("EMPTY", "files is required", List.of()));
        }

        String uploaderUuid = authentication == null ? null : authentication.getName();
        List<StoredFile> result = new ArrayList<>();
        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) {
                continue;
            }
            StoredFile saved = switch (type) {
                case image ->
                    fileStorageService.storeImage(f, uploaderUuid);
                case video ->
                    fileStorageService.storeVideo(f, uploaderUuid);
                case auto ->
                    fileStorageService.storeAuto(f, uploaderUuid);
            };
            result.add(saved);
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "이미지 업로드", description = "이미지 파일을 업로드하고 공개 URL을 반환합니다.")
    @PostMapping(path = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<StoredFile>> uploadImage(
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) throws Exception {
        String uploaderUuid = authentication == null ? null : authentication.getName();
        StoredFile saved = fileStorageService.storeImage(file, uploaderUuid);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @Operation(summary = "영상 업로드", description = "영상 파일을 업로드하고 공개 URL을 반환합니다.")
    @PostMapping(path = "/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<StoredFile>> uploadVideo(
            @Parameter(description = "업로드할 영상 파일", required = true)
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) throws Exception {
        String uploaderUuid = authentication == null ? null : authentication.getName();
        StoredFile saved = fileStorageService.storeVideo(file, uploaderUuid);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    private static boolean hasAdminRole(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (ga != null && "ROLE_ADMIN".equalsIgnoreCase(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private static void ensureAuthenticated(Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }
    }

    @Operation(summary = "S3 오브젝트 목록(aws s3 ls 형태)", description = "prefix 기준으로 오브젝트 목록을 조회합니다. 보안상 ADMIN만 사용할 수 있습니다.")
    @GetMapping(path = "/s3")
    public ResponseEntity<ApiResponse<List<S3ListItem>>> listS3Objects(
            @Parameter(description = "(ADMIN 전용) S3 prefix. 예: _test/")
            @RequestParam(name = "prefix", required = false, defaultValue = "") String prefix,
            @Parameter(description = "(ADMIN 전용) 최대 반환 개수(최대 1000)")
            @RequestParam(name = "maxKeys", required = false, defaultValue = "100") int maxKeys,
            @Parameter(description = "(일반 사용자) 내 업로드 파일 목록 개수(최대 200)")
            @RequestParam(name = "limit", required = false, defaultValue = "50") int limit,
            Authentication authentication
    ) {
        ensureAuthenticated(authentication);

        boolean isAdmin = hasAdminRole(authentication);
        if (!isAdmin) {
            throw new AccessDeniedException("관리자만 S3 목록을 조회할 수 있습니다.");
        }

        List<S3ObjectStorage.ListedObject> items = s3ObjectStorage.list(s3ObjectStorage.bucket(), prefix, maxKeys);
        List<S3ListItem> mapped = new ArrayList<>();
        for (S3ObjectStorage.ListedObject o : items) {
            mapped.add(new S3ListItem(
                    o.key(),
                    o.sizeBytes(),
                    o.lastModified(),
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }
        return ResponseEntity.ok(ApiResponse.success(mapped));
    }

    @Operation(summary = "S3 오브젝트 삭제(aws s3 rm 형태)", description = "S3 오브젝트를 삭제합니다. 보안상 ADMIN만 사용할 수 있습니다.")
    @DeleteMapping(path = "/s3")
    public ResponseEntity<ApiResponse<Void>> deleteS3Object(
            @Parameter(description = "(ADMIN 전용) S3 key. 예: _test/s3-test.txt")
            @RequestParam(name = "key", required = false) String key,
            @Parameter(description = "(일반/ADMIN) folder(images/videos)")
            @RequestParam(name = "folder", required = false) String folder,
            @Parameter(description = "(일반/ADMIN) storedName")
            @RequestParam(name = "storedName", required = false) String storedName,
            Authentication authentication
    ) {
        ensureAuthenticated(authentication);
        boolean isAdmin = hasAdminRole(authentication);
        if (!isAdmin) {
            throw new AccessDeniedException("관리자만 S3 삭제를 수행할 수 있습니다.");
        }

        if (key != null && !key.isBlank()) {
            s3ObjectStorage.delete(s3ObjectStorage.bucket(), key);
            return ResponseEntity.ok(ApiResponse.success());
        }

        if (folder == null || folder.isBlank() || storedName == null || storedName.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "key 또는 folder/storedName이 필요합니다."));
        }

        fileStorageService.deleteOrThrow(folder, storedName);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
