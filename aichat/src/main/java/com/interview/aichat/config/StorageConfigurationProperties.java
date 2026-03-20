package com.interview.aichat.config;

import com.interview.aichat.file.StorageMode;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aichat.storage")
@RefreshScope
public class StorageConfigurationProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String region = "kevin_1";
    private StorageMode mode = StorageMode.S3;
    private String localBasePath = "/tmp/aichat-storage";
}
