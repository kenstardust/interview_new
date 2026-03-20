package com.interview.aichat.knowledge.storage;

import com.interview.aichat.config.StorageConfigurationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kb.storage", name = "backend", havingValue = "S3")
public class S3ObjectStorage implements ObjectStorage {

    private final S3Client s3Client;
    private final StorageConfigurationProperties storageProperties;

    @Override
    public StorageBackendType backendType() {
        return StorageBackendType.S3;
    }

    @Override
    public String put(StorageObject object) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(object.getKey())
                .contentType(object.getContentType())
                .contentLength((long) object.getContent().length)
                .build();
        s3Client.putObject(putRequest, RequestBody.fromBytes(object.getContent()));
        return object.getKey();
    }

    @Override
    public StorageObject get(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(key)
                .build();
        return StorageObject.builder()
                .key(key)
                .contentType("application/octet-stream")
                .content(s3Client.getObjectAsBytes(request).asByteArray())
                .build();
    }

    @Override
    public void delete(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(key)
                .build();
        s3Client.deleteObject(request);
    }
}
