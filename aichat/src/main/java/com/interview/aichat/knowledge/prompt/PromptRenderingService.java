package com.interview.aichat.knowledge.prompt;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PromptRenderingService {

    private final ResourceLoader resourceLoader;

    public String render(String templateName, Map<String, Object> variables) {
        String templateContent = loadFromDasspath(templateName);
        PromptTemplate template = new PromptTemplate(templateContent);
        return template.render(variables);
    }

    public String jsonFormatInstruction() {
        BeanOutputConverter<AnswerPayload> converter = new BeanOutputConverter<>(AnswerPayload.class);
        return converter.getFormat();
    }

    private String loadFromDasspath(String templateName) {
        try {
            Resource resource = resourceLoader.getResource("classpath:prompts/" + templateName + ".st");
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("提示词模板加载失败: " + templateName, e);
        }
    }
}
