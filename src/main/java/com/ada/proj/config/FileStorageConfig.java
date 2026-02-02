package com.ada.proj.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FileStorageConfig implements WebMvcConfigurer {

    private final String baseDir;

    public FileStorageConfig(@Value("${app.storage.base-dir:uploads}") String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // NOTE:
        // 기존에는 uploads 디렉토리를 "/files/**"로 정적 서빙했으나, 현재는 업로드 파일을 DB(BLOB)에 저장하고
        // [PublicFilesController]가 "/files/{folder}/{storedName}"를 스트리밍으로 제공한다.
        // 디스크 기반 정적 서빙은 민감 파일이 섞일 경우 위험하므로 비활성화한다.
    }
}
