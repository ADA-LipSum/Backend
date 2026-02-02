package com.ada.proj.controller;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.service.FileStorageService;
import com.ada.proj.service.FileStorageService.StoredFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@Tag(name = "파일 업로드", description = "이미지/영상 파일을 업로드하고 URL을 반환하는 API")
public class FilesController {

    private final FileStorageService fileStorageService;

    public FilesController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public enum UploadType {
        auto,
        image,
        video
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
}
