package com.interview.aichat.stream;

import com.interview.kevin.constant.TaskStatusCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskStatusService {

    private final StringRedisTemplate redisTemplate;
    private final RedisStreamProperties streamProperties;

    public void initPending(String taskId, String taskType, String fileKey) {
        Map<String, String> status = new LinkedHashMap<>();
        status.put("taskId", taskId);
        status.put("taskType", taskType);
        status.put("fileKey", fileKey);
        status.put("status", TaskStatusCode.PENDING.name());
        status.put("updatedAt", Instant.now().toString());
        redisTemplate.opsForHash().putAll(statusKey(taskId), status);
    }

    public void update(String taskId, TaskStatusCode status) {
        update(taskId, status, null);
    }

    public void update(String taskId, TaskStatusCode status, String message) {
        Map<String, String> updates = new LinkedHashMap<>();
        updates.put("status", status.name());
        updates.put("updatedAt", Instant.now().toString());
        if (message != null && !message.isBlank()) {
            updates.put("message", message);
        }
        redisTemplate.opsForHash().putAll(statusKey(taskId), updates);
    }

    public Map<Object, Object> getStatus(String taskId) {
        return redisTemplate.opsForHash().entries(statusKey(taskId));
    }

    public String statusKey(String taskId) {
        return streamProperties.getStatusKeyPrefix() + taskId;
    }
}
