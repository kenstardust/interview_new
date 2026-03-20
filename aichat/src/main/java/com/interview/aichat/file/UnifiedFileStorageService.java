package com.interview.aichat.file;

import com.interview.aichat.config.StorageConfigurationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UnifiedFileStorageService {

    private final StorageConfigurationProperties storageProperties;
    private final LocalFileStorageService localFileStorageService;
    private final FileStorageService fileStorageService;

    public String put(StoredObject object) {
        if (storageProperties.getMode() == StorageMode.LOCAL) {
            return localFileStorageService.put(object);
        }
        return fileStorageService.uploadBytes(object.getContent(), object.getKey(), "kb", object.getContentType());
    }

    public StoredObject get(String key) {
        if (storageProperties.getMode() == StorageMode.LOCAL) {
            return localFileStorageService.get(key);
        }
        return StoredObject.builder()
                .key(key)
                .content(fileStorageService.downloadFile(key))
                .contentType("application/octet-stream")
                .build();
    }

    public void delete(String key) {
        if (storageProperties.getMode() == StorageMode.LOCAL) {
            localFileStorageService.delete(key);
            return;
        }
        fileStorageService.deleteFile(key);
    }
}
