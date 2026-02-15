package com.ada.proj.repository;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;

@Repository
public class UploadedFileJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public UploadedFileJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record FileMeta(
            String storedName,
            String folder,
            String originalName,
            String contentType,
            long sizeBytes,
            String uploaderUuid,
            String storageProvider,
            String s3Bucket,
            String s3Key
            ) {

    }

    public void saveS3Ref(
            @NonNull String storedName,
            @NonNull String folder,
            @NonNull String originalName,
            @NonNull String contentType,
            long sizeBytes,
            @NonNull String s3Bucket,
            @NonNull String s3Key,
            String uploaderUuid
    ) {
        String sql = "INSERT INTO uploaded_files (stored_name, folder, original_name, content_type, size_bytes, data, uploader_uuid, created_at, storage_provider, s3_bucket, s3_key) "
                + "VALUES (?, ?, ?, ?, ?, NULL, ?, ?, 'S3', ?, ?)";

        Instant now = Instant.now();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, storedName);
            ps.setString(2, folder);
            ps.setString(3, originalName);
            ps.setString(4, contentType);
            ps.setLong(5, sizeBytes);
            ps.setString(6, uploaderUuid);
            ps.setTimestamp(7, Timestamp.from(now));
            ps.setString(8, s3Bucket);
            ps.setString(9, s3Key);
            return ps;
        });
    }

    public FileMeta findMetaOrThrow(@NonNull String folder, @NonNull String storedName) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT stored_name, folder, original_name, content_type, size_bytes, uploader_uuid, storage_provider, s3_bucket, s3_key FROM uploaded_files WHERE folder = ? AND stored_name = ?",
                    (rs, rowNum) -> new FileMeta(
                            rs.getString("stored_name"),
                            rs.getString("folder"),
                            rs.getString("original_name"),
                            rs.getString("content_type"),
                            rs.getLong("size_bytes"),
                            rs.getString("uploader_uuid"),
                            rs.getString("storage_provider"),
                            rs.getString("s3_bucket"),
                            rs.getString("s3_key")
                    ),
                    folder,
                    storedName
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new EntityNotFoundException("File not found: " + folder + "/" + storedName);
        }
    }

    public int deleteByFolderAndStoredName(@NonNull String folder, @NonNull String storedName) {
        return jdbcTemplate.update(
                "DELETE FROM uploaded_files WHERE folder = ? AND stored_name = ?",
                folder,
                storedName
        );
    }

    public List<FileMeta> listByUploader(String uploaderUuid, int limit) {
        if (limit <= 0) {
            limit = 20;
        }
        if (limit > 200) {
            limit = 200;
        }

        return jdbcTemplate.query(
                "SELECT stored_name, folder, original_name, content_type, size_bytes, uploader_uuid, storage_provider, s3_bucket, s3_key "
                + "FROM uploaded_files WHERE uploader_uuid = ? ORDER BY created_at DESC LIMIT ?",
                (rs, rowNum) -> new FileMeta(
                        rs.getString("stored_name"),
                        rs.getString("folder"),
                        rs.getString("original_name"),
                        rs.getString("content_type"),
                        rs.getLong("size_bytes"),
                        rs.getString("uploader_uuid"),
                        rs.getString("storage_provider"),
                        rs.getString("s3_bucket"),
                        rs.getString("s3_key")
                ),
                uploaderUuid,
                limit
        );
    }
}
