package com.interview.aichat.knowledge.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class KnowledgeAskResponse {
    private String taskId;
    private String sessionId;
    private String status;
    private Instant acceptedAt;
}
