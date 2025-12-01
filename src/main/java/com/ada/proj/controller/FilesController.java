package com.ada.proj.controller;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.service.FileStorageService;
import com.ada.proj.service.FileStorageService.StoredFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@Tag(name = "파일 업로드", description = "이미지/영상 파일을 업로드하고 URL을 반환하는 API")
public class FilesController {

    private final FileStorageService fileStorageService;

    public FilesController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Operation(summary = "이미지 업로드", description = "이미지 파일을 업로드하고 공개 URL을 반환합니다.")
    @PostMapping(path = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<StoredFile>> uploadImage(
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestPart("file") MultipartFile file
    ) throws Exception {
        StoredFile saved = fileStorageService.storeImage(file);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @Operation(summary = "영상 업로드", description = "영상 파일을 업로드하고 공개 URL을 반환합니다.")
    @PostMapping(path = "/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<StoredFile>> uploadVideo(
            @Parameter(description = "업로드할 영상 파일", required = true)
            @RequestPart("file") MultipartFile file
    ) throws Exception {
        StoredFile saved = fileStorageService.storeVideo(file);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }
}
