package com.ada.proj.config;

import java.nio.file.Path;
import java.nio.file.Paths;

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
        Path root = Paths.get(baseDir).toAbsolutePath().normalize();
        String location = root.toUri().toString();
        // 예: http://host/files/xxxx 로 접근 가능
        registry.addResourceHandler("/files/**")
                .addResourceLocations(location)
                .setCachePeriod(60 * 60) // 1 hour
                .resourceChain(true);
    }
}
