package com.industry.aichat.service.graph;

import com.industry.aichat.model.DocumentChunk;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphAnswerContext {

    @Builder.Default
    private List<GraphEvidence> graphEvidence = new ArrayList<>();

    @Builder.Default
    private List<DocumentChunk> fallbackChunks = new ArrayList<>();

    public boolean hasEvidence() {
        return !graphEvidence.isEmpty() || !fallbackChunks.isEmpty();
    }
}
