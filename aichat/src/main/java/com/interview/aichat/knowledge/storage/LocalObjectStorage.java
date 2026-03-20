package com.interview.aichat.knowledge.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Primary
public class LocalObjectStorage implements ObjectStorage {

    @Value("${kb.storage.local-path:/tmp/kb-storage}")
    private String rootPath;

    @Override
    public StorageBackendType backendType() {
        return StorageBackendType.LOCAL;
    }

    @Override
    public String put(StorageObject object) {
        try {
            Path path = Path.of(rootPath, object.getKey());
            Files.createDirectories(path.getParent());
            Files.write(path, object.getContent());
            return object.getKey();
        } catch (IOException e) {
            throw new IllegalStateException("本地存储写入失败", e);
        }
    }

    @Override
    public StorageObject get(String key) {
        try {
            Path path = Path.of(rootPath, key);
            return StorageObject.builder()
                    .key(key)
                    .content(Files.readAllBytes(path))
                    .contentType("application/octet-stream")
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException("本地存储读取失败", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(Path.of(rootPath, key));
        } catch (IOException e) {
            throw new IllegalStateException("本地存储删除失败", e);
        }
    }
}
