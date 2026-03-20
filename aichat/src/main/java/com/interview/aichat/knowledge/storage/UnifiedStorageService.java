package com.interview.aichat.knowledge.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UnifiedStorageService {

    private final ObjectStorage activeStorage;

    public UnifiedStorageService(List<ObjectStorage> storages,
                                 @Value("${kb.storage.backend:LOCAL}") StorageBackendType backendType) {
        this.activeStorage = storages.stream()
                .filter(storage -> storage.backendType() == backendType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到可用存储后端: " + backendType));
    }

    public String put(StorageObject object) {
        return activeStorage.put(object);
    }

    public StorageObject get(String key) {
        return activeStorage.get(key);
    }

    public void delete(String key) {
        activeStorage.delete(key);
    }
}
