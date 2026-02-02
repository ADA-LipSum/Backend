package com.ada.proj.repository;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StreamUtils;

import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;

@Repository
public class UploadedFileJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public UploadedFileJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record FileMeta(String storedName, String folder, String originalName, String contentType, long sizeBytes) {

    }

    public void save(
            @NonNull String storedName,
            @NonNull String folder,
            @NonNull String originalName,
            @NonNull String contentType,
            long sizeBytes,
            @NonNull InputStream dataStream,
            String uploaderUuid
    ) {
        String sql = "INSERT INTO uploaded_files (stored_name, folder, original_name, content_type, size_bytes, data, uploader_uuid, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Instant now = Instant.now();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, storedName);
            ps.setString(2, folder);
            ps.setString(3, originalName);
            ps.setString(4, contentType);
            ps.setLong(5, sizeBytes);
            ps.setBinaryStream(6, dataStream, sizeBytes);
            ps.setString(7, uploaderUuid);
            ps.setTimestamp(8, Timestamp.from(now));
            return ps;
        });
    }

    public FileMeta findMetaOrThrow(@NonNull String folder, @NonNull String storedName) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT stored_name, folder, original_name, content_type, size_bytes FROM uploaded_files WHERE folder = ? AND stored_name = ?",
                    (rs, rowNum) -> new FileMeta(
                            rs.getString("stored_name"),
                            rs.getString("folder"),
                            rs.getString("original_name"),
                            rs.getString("content_type"),
                            rs.getLong("size_bytes")
                    ),
                    folder,
                    storedName
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new EntityNotFoundException("File not found: " + folder + "/" + storedName);
        }
    }

    public void streamDataOrThrow(@NonNull String folder, @NonNull String storedName, @NonNull OutputStream outputStream) {
        String sql = "SELECT data FROM uploaded_files WHERE folder = ? AND stored_name = ?";

        Integer rows = jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, folder);
            ps.setString(2, storedName);
            return ps;
        }, rs -> {
            if (!rs.next()) {
                return 0;
            }
            InputStream in = rs.getBinaryStream(1);
            if (in == null) {
                return 0;
            }
            try {
                StreamUtils.copy(in, outputStream);
            } catch (java.io.IOException io) {
                throw new UncheckedIOException(io);
            } finally {
                try {
                    in.close();
                } catch (java.io.IOException ignored) {
                    // ignore
                }
            }
            return 1;
        });

        if (rows == null || rows == 0) {
            throw new EntityNotFoundException("File not found: " + folder + "/" + storedName);
        }
    }
}
