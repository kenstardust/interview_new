package com.interview.aichat.knowledge.storage;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StorageObject {
    private String key;
    private byte[] content;
    private String contentType;
}
