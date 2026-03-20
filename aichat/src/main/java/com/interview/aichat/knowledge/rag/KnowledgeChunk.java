package com.interview.aichat.knowledge.rag;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeChunk {
    private Long id;
    private String documentId;
    private String content;
    private double score;
}
