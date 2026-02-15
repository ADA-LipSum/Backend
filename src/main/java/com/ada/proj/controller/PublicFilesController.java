package com.ada.proj.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.ada.proj.repository.UploadedFileJdbcRepository.FileMeta;
import com.ada.proj.service.FileStorageService;

@RestController
public class PublicFilesController {

    private final FileStorageService fileStorageService;

    public PublicFilesController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/files/{folder}/{storedName}")
    public ResponseEntity<StreamingResponseBody> getFile(
            @PathVariable("folder") String folder,
            @PathVariable("storedName") String storedName
    ) {
        // 폴더 allowlist(경로탐색/정책 우회 방지)
        if (!"images".equals(folder) && !"videos".equals(folder)) {
            return ResponseEntity.notFound().build();
        }

        FileMeta meta = fileStorageService.getMetaOrThrow(folder, storedName);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(meta.contentType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        StreamingResponseBody body = outputStream
                -> fileStorageService.streamToOrThrow(folder, storedName, outputStream);

        ContentDisposition disposition = ContentDisposition.inline().filename(meta.originalName()).build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(meta.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }
}
