package com.interview.aichat.file;

import com.interview.aichat.config.StorageConfigurationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService {

    private final StorageConfigurationProperties storageProperties;

    public String put(StoredObject object) {
        try {
            Path path = Path.of(storageProperties.getLocalBasePath(), object.getKey());
            Files.createDirectories(path.getParent());
            Files.write(path, object.getContent());
            return object.getKey();
        } catch (IOException e) {
            throw new IllegalStateException("本地文件存储失败", e);
        }
    }

    public StoredObject get(String key) {
        try {
            byte[] content = Files.readAllBytes(Path.of(storageProperties.getLocalBasePath(), key));
            return StoredObject.builder()
                    .key(key)
                    .content(content)
                    .contentType("application/octet-stream")
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException("本地文件读取失败", e);
        }
    }

    public void delete(String key) {
        try {
            Files.deleteIfExists(Path.of(storageProperties.getLocalBasePath(), key));
        } catch (IOException e) {
            throw new IllegalStateException("本地文件删除失败", e);
        }
    }
}
