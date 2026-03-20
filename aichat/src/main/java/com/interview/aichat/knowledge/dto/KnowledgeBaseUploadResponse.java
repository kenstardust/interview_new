package com.interview.aichat.knowledge.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeBaseUploadResponse {
    private String documentId;
    private int chunks;
    private boolean overridden;
}

