package com.interview.aichat.knowledge.controller;

import com.interview.aichat.knowledge.dto.KnowledgeBaseUploadResponse;
import com.interview.aichat.knowledge.ingest.KnowledgeBaseIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ai/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeBaseUploadController {

    private final KnowledgeBaseIngestionService ingestionService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KnowledgeBaseUploadResponse upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentId", required = false) String documentId,
            @RequestParam(value = "override", defaultValue = "false") boolean override,
            @RequestParam(value = "chunkSize", defaultValue = "1200") int chunkSize,
            @RequestParam(value = "chunkOverlap", defaultValue = "200") int chunkOverlap
    ) {
        return ingestionService.ingest(file, documentId, override, chunkSize, chunkOverlap);
    }
}

