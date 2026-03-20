package com.interview.aichat.stream;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StreamTaskPublisher {

    private final StringRedisTemplate redisTemplate;
    private final RedisStreamProperties streamProperties;

    public String publish(String taskId, String taskType, String storageKey, String fileName) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("taskId", taskId);
        body.put("taskType", taskType);
        body.put("storageKey", storageKey);
        body.put("fileName", fileName == null ? "unknown" : fileName);

        RecordId recordId = redisTemplate.opsForStream().add(
                StreamRecords.mapBacked(body).withStreamKey(streamProperties.getKey())
        );

        return recordId == null ? null : recordId.getValue();
    }
}
