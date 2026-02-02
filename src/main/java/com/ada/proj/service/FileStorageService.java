package com.ada.proj.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ada.proj.repository.UploadedFileJdbcRepository;

@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 500L * 1024 * 1024; // 500MiB

    private final UploadedFileJdbcRepository uploadedFileJdbcRepository;

    private static final Set<String> IMAGE_EXTS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final Set<String> VIDEO_EXTS = Set.of("mp4", "mov", "avi", "mkv", "webm");

    public FileStorageService(
            UploadedFileJdbcRepository uploadedFileJdbcRepository,
            @Value("${app.storage.base-dir:uploads}") String ignoredBaseDir
    ) {
        this.uploadedFileJdbcRepository = uploadedFileJdbcRepository;
    }

    public StoredFile storeImage(MultipartFile file) throws IOException {
        return store(file, "images", IMAGE_EXTS);
    }

    public StoredFile storeVideo(MultipartFile file) throws IOException {
        return store(file, "videos", VIDEO_EXTS);
    }

    private StoredFile store(MultipartFile file, String folder, Set<String> allowedExts) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }

        long size = file.getSize();
        if (size <= 0) {
            throw new IllegalArgumentException("Empty file");
        }
        if (size > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File too large (max 500MB)");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            originalName = "file";
        }
        String original = StringUtils.cleanPath(originalName);
        String ext = getExtension(original).toLowerCase();
        if (!allowedExts.contains(ext)) {
            throw new IllegalArgumentException("Unsupported file extension: ." + ext);
        }

        String storedName = UUID.randomUUID() + (ext.isEmpty() ? "" : ("." + ext));
        String url = "/files/" + folder + "/" + storedName;

        String contentType = normalizeContentType(folder, ext, file.getContentType());

        try (BufferedInputStream in = new BufferedInputStream(file.getInputStream())) {
            in.mark(64 * 1024);
            byte[] header = in.readNBytes(64);
            in.reset();

            validateMagicNumber(folder, ext, header);

            uploadedFileJdbcRepository.save(
                    storedName,
                    folder,
                    original,
                    contentType,
                    size,
                    in,
                    null
            );
        }

        return new StoredFile(original, storedName, url, size, contentType);
    }

    private String normalizeContentType(String folder, String ext, String fallback) {
        // 확장자 + 폴더 정책 우선. fallback은 참고만(스푸핑 가능)
        String ct = switch (ext) {
            case "jpg", "jpeg" ->
                MediaType.IMAGE_JPEG_VALUE;
            case "png" ->
                MediaType.IMAGE_PNG_VALUE;
            case "gif" ->
                MediaType.IMAGE_GIF_VALUE;
            case "webp" ->
                "image/webp";
            case "bmp" ->
                "image/bmp";
            case "mp4" ->
                "video/mp4";
            case "webm" ->
                "video/webm";
            case "mov" ->
                "video/quicktime";
            case "avi" ->
                "video/x-msvideo";
            case "mkv" ->
                "video/x-matroska";
            default ->
                null;
        };
        if (ct != null) {
            return ct;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        if ("images".equals(folder)) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private void validateMagicNumber(String folder, String ext, byte[] header) {
        if (header == null || header.length == 0) {
            throw new IllegalArgumentException("Invalid file");
        }

        boolean ok = switch (ext) {
            case "jpg", "jpeg" ->
                startsWith(header, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF);
            case "png" ->
                startsWith(header, (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "gif" ->
                startsWithAscii(header, "GIF87a") || startsWithAscii(header, "GIF89a");
            case "bmp" ->
                startsWithAscii(header, "BM");
            case "webp" ->
                isWebp(header);
            case "mp4", "mov" ->
                hasFtypBox(header);
            case "avi" ->
                isRiffType(header, "AVI ");
            case "webm", "mkv" ->
                startsWith(header, 0x1A, 0x45, 0xDF, 0xA3); // EBML
            default ->
                false;
        };

        if (!ok) {
            throw new IllegalArgumentException("File content does not match extension");
        }

        // 폴더 정책과 확장자 조합도 재확인(혹시 호출자가 잘못된 folder를 넘길 경우)
        if ("images".equals(folder) && !IMAGE_EXTS.contains(ext)) {
            throw new IllegalArgumentException("Invalid folder/file type");
        }
        if ("videos".equals(folder) && !VIDEO_EXTS.contains(ext)) {
            throw new IllegalArgumentException("Invalid folder/file type");
        }
    }

    private static boolean startsWith(byte[] header, int... bytes) {
        if (header.length < bytes.length) {
            return false;
        }
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            if ((header[i] & 0xFF) != b) {
                return false;
            }
        }
        return true;
    }

    private static boolean startsWithAscii(byte[] header, String ascii) {
        byte[] bytes = ascii.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        if (header.length < bytes.length) {
            return false;
        }
        for (int i = 0; i < bytes.length; i++) {
            if (header[i] != bytes[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWebp(byte[] header) {
        // RIFF....WEBP
        if (header.length < 12) {
            return false;
        }
        return startsWithAscii(header, "RIFF") && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
    }

    private static boolean hasFtypBox(byte[] header) {
        // ISO BMFF: size(4) + 'ftyp'(4)
        if (header.length < 12) {
            return false;
        }
        return header[4] == 'f' && header[5] == 't' && header[6] == 'y' && header[7] == 'p';
    }

    private static boolean isRiffType(byte[] header, String fourCC) {
        if (header.length < 12) {
            return false;
        }
        if (!(header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F')) {
            return false;
        }
        byte[] t = fourCC.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        return header[8] == t[0] && header[9] == t[1] && header[10] == t[2] && header[11] == t[3];
    }

    private static String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return (i == -1) ? "" : filename.substring(i + 1);
    }

    public record StoredFile(String originalName, String storedName, String url, long size, String contentType) {

    }
}
