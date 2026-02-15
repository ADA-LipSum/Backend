package com.ada.proj.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.ada.proj.config.S3StorageProperties;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3ObjectStorage {

    private final S3Client s3Client;
    private final S3StorageProperties props;

    public S3ObjectStorage(S3Client s3Client, S3StorageProperties props) {
        this.s3Client = s3Client;
        this.props = props;
    }

    public String bucket() {
        return props.getBucket();
    }

    public String keyPrefix() {
        return props.getKeyPrefix();
    }

    public String buildKey(String folder, String storedName) {
        String prefix = props.getKeyPrefix();
        if (prefix == null) {
            prefix = "";
        }
        prefix = prefix.trim();
        if (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }

        String key = folder + "/" + storedName;
        if (prefix.isEmpty()) {
            return key;
        }
        return prefix + "/" + key;
    }

    public void upload(String folder, String storedName, String contentType, long sizeBytes, InputStream in) {
        if (props.getBucket() == null || props.getBucket().isBlank()) {
            throw new IllegalStateException("app.storage.s3.bucket is required in s3 mode");
        }

        String key = buildKey(folder, storedName);

        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .contentType(contentType)
                .contentLength(sizeBytes)
                .build();

        s3Client.putObject(req, RequestBody.fromInputStream(in, sizeBytes));
    }

    public void downloadTo(String bucket, String key, OutputStream out) {
        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        try (var in = s3Client.getObject(req)) {
            StreamUtils.copy(in, out);
        } catch (java.io.IOException io) {
            throw new java.io.UncheckedIOException(io);
        }
    }

    public record ListedObject(String key, long sizeBytes, Instant lastModified) {

    }

    public List<ListedObject> list(String bucket, String prefix, int maxKeys) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("bucket is required");
        }
        if (maxKeys <= 0) {
            maxKeys = 100;
        }
        if (maxKeys > 1000) {
            maxKeys = 1000;
        }

        List<ListedObject> out = new ArrayList<>();

        String token = null;
        do {
            ListObjectsV2Request.Builder req = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .maxKeys(Math.min(1000, maxKeys - out.size()));
            if (prefix != null && !prefix.isBlank()) {
                req = req.prefix(prefix);
            }
            if (token != null) {
                req = req.continuationToken(token);
            }

            ListObjectsV2Response res = s3Client.listObjectsV2(req.build());
            if (res.contents() != null) {
                for (var o : res.contents()) {
                    if (out.size() >= maxKeys) {
                        break;
                    }
                    out.add(new ListedObject(
                            o.key(),
                            o.size(),
                            o.lastModified()
                    ));
                }
            }

            token = res.isTruncated() ? res.nextContinuationToken() : null;
        } while (token != null && out.size() < maxKeys);

        return out;
    }

    public void delete(String bucket, String key) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("bucket is required");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key is required");
        }

        DeleteObjectRequest req = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        s3Client.deleteObject(req);
    }
}
