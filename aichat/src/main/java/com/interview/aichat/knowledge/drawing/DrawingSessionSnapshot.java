package com.interview.aichat.knowledge.drawing;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DrawingSessionSnapshot {
    private String sessionId;
    private DrawingSessionState state;
    private String answerDraft;
    private Instant updatedAt;
}
