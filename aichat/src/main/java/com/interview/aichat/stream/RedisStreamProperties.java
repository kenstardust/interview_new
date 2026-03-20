package com.interview.aichat.stream;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "aichat.stream")
public class RedisStreamProperties {
    private String key = "knowledgebase:tasks";
    private String group = "kg-consumer-group";
    private String statusKeyPrefix = "task:status:";
    private long pendingIdleMs = 60_000;
}
