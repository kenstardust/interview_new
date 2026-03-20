package com.interview.aichat.knowledge.controller;

import com.interview.aichat.knowledge.KnowledgeQaOrchestrator;
import com.interview.aichat.knowledge.drawing.DrawingSessionLifecycleService;
import com.interview.aichat.knowledge.dto.KnowledgeAskRequest;
import com.interview.aichat.knowledge.dto.KnowledgeAskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeQaController {

    private final KnowledgeQaOrchestrator orchestrator;
    private final DrawingSessionLifecycleService drawingSessionLifecycleService;

    @PostMapping("/ask")
    public KnowledgeAskResponse ask(@RequestBody KnowledgeAskRequest request) {
        return orchestrator.ask(request);
    }

    @GetMapping("/drawing/{sessionId}/resume")
    public Object resume(@PathVariable String sessionId) {
        return drawingSessionLifecycleService.resume(sessionId);
    }
}
