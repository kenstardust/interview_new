package com.interview.aichat.knowledge.domain;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class GenerationTask {
    private String taskId;
    private String sessionId;
    private String question;
    private Map<String, Object> metadata;
    private Instant createdAt;
}
