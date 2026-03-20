package com.interview.aichat.knowledge.storage;

public interface ObjectStorage {

    StorageBackendType backendType();

    String put(StorageObject object);

    StorageObject get(String key);

    void delete(String key);
}
