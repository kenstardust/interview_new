package com.interview.aichat.file;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StoredObject {
    private String key;
    private byte[] content;
    private String contentType;
}
