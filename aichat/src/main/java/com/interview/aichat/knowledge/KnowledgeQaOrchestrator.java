package com.interview.aichat.knowledge;

import com.interview.aichat.knowledge.async.RedisStreamAsyncPipeline;
import com.interview.aichat.knowledge.domain.GenerationTask;
import com.interview.aichat.knowledge.drawing.DrawingSessionLifecycleService;
import com.interview.aichat.knowledge.drawing.DrawingSessionState;
import com.interview.aichat.knowledge.dto.KnowledgeAskRequest;
import com.interview.aichat.knowledge.dto.KnowledgeAskResponse;
import com.interview.aichat.knowledge.prompt.PromptRenderingService;
import com.interview.aichat.knowledge.rag.PgVectorRetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeQaOrchestrator {

    private final RedisStreamAsyncPipeline asyncPipeline;
    private final PgVectorRetrievalService retrievalService;
    private final PromptRenderingService promptRenderingService;
    private final DrawingSessionLifecycleService drawingSessionLifecycleService;

    public KnowledgeAskResponse ask(KnowledgeAskRequest request) {
        var chunks = retrievalService.search(request.getQuestion(), 5);
        String context = chunks.stream()
                .map(c -> "[%s] %s".formatted(c.getDocumentId(), c.getContent()))
                .collect(Collectors.joining("\n"));

        String prompt = promptRenderingService.render(request.getTemplate(), Map.of(
                "question", request.getQuestion(),
                "context", context,
                "format", promptRenderingService.jsonFormatInstruction()
        ));

        String taskId = UUID.randomUUID().toString();
        asyncPipeline.submit(GenerationTask.builder()
                .taskId(taskId)
                .sessionId(request.getSessionId())
                .question(prompt)
                .createdAt(Instant.now())
                .metadata(Map.of("question", request.getQuestion()))
                .build());

        drawingSessionLifecycleService.updateState(request.getSessionId(), DrawingSessionState.GENERATING, "");

        return KnowledgeAskResponse.builder()
                .taskId(taskId)
                .sessionId(request.getSessionId())
                .status("ACCEPTED")
                .acceptedAt(Instant.now())
                .build();
    }
}
