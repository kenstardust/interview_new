package com.industry.aichat.config;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@Component
@ConfigurationProperties(prefix = "aichat.storage")
public class StorageConfigurationProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String region = "kevin_1";
}
