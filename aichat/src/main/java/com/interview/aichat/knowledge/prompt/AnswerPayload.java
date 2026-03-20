package com.interview.aichat.knowledge.prompt;

import lombok.Data;

import java.util.List;

@Data
public class AnswerPayload {
    private String answer;
    private List<String> citations;
    private String sessionState;
}
