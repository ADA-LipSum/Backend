package com.ada.proj.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "uploaded_files",
        indexes = {
            @Index(name = "idx_uploaded_files_folder", columnList = "folder")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class UploadedFile {

    @Id
    @Column(name = "stored_name", length = 100, nullable = false)
    private String storedName;

    @Column(name = "folder", length = 20, nullable = false)
    private String folder;

    @Column(name = "original_name", length = 255, nullable = false)
    private String originalName;

    @Column(name = "content_type", length = 100, nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Lob
    @Column(name = "data", columnDefinition = "LONGBLOB")
    private byte[] data;

    /**
     * 저장소 타입: DB 또는 S3
     */
    @Column(name = "storage_provider", length = 10, nullable = false)
    private String storageProvider = "DB";

    @Column(name = "s3_bucket", length = 128)
    private String s3Bucket;

    @Column(name = "s3_key", length = 512)
    private String s3Key;

    @Column(name = "uploader_uuid", length = 64)
    private String uploaderUuid;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
