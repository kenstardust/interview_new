package com.interview.aichat.knowledge.dto;

import lombok.Data;

@Data
public class KnowledgeAskRequest {
    private String sessionId;
    private String question;
    private String template = "knowledgebase-query-user";
}
