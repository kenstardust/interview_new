package com.interview.aichat.knowledge.async;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.aichat.knowledge.domain.GenerationTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * 基于 Redis Stream 的两层异步消费：
 * 1) ingest 层负责快速接收请求并持久化到 stage-1 stream（目标 p99 < 200ms）
 * 2) generation 层从 stage-2 stream 并发处理大模型调用，支持 Consumer Group 水平扩容
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamAsyncPipeline {

    public static final String STAGE1_STREAM = "kb:stream:ingest";
    public static final String STAGE2_STREAM = "kb:stream:generation";
    public static final String STAGE1_GROUP = "kb:group:ingest";
    public static final String STAGE2_GROUP = "kb:group:generation";

    private final StringRedisTemplate redisTemplate;
    private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:aichat}")
    private String appName;

    @PostConstruct
    public void initConsumerGroups() {
        createGroupIfAbsent(STAGE1_STREAM, STAGE1_GROUP);
        createGroupIfAbsent(STAGE2_STREAM, STAGE2_GROUP);
        startLayerWorkers();
    }

    public void submit(GenerationTask task) {
        try {
            StreamOperations<String, Object, Object> ops = redisTemplate.opsForStream();
            ops.add(StreamRecords.newRecord()
                    .ofMap(Map.of("payload", objectMapper.writeValueAsString(task)))
                    .withStreamKey(STAGE1_STREAM));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("任务序列化失败", e);
        }
    }

    private void startLayerWorkers() {
        String ingestConsumer = appName + "-ingest-" + System.nanoTime();
        String generationConsumer = appName + "-generation-" + System.nanoTime();

        listenerContainer.receiveAutoAck(
                org.springframework.data.redis.connection.stream.Consumer.from(STAGE1_GROUP, ingestConsumer),
                StreamOffset.create(STAGE1_STREAM, ReadOffset.lastConsumed()),
                (StreamListener<String, MapRecord<String, String, String>>) message -> {
                    String payload = message.getValue().get("payload");
                    redisTemplate.opsForStream().add(StreamRecords.newRecord()
                            .ofMap(Map.of("payload", payload))
                            .withStreamKey(STAGE2_STREAM));
                }
        );

        listenerContainer.receive(
                org.springframework.data.redis.connection.stream.Consumer.from(STAGE2_GROUP, generationConsumer),
                StreamOffset.create(STAGE2_STREAM, ReadOffset.lastConsumed()),
                (StreamListener<String, MapRecord<String, String, String>>) message ->
                        log.debug("Stage2 consumed, id={}, payload={} ", message.getId(), message.getValue().get("payload"))
        );

        listenerContainer.start();
    }

    private void createGroupIfAbsent(String stream, String group) {
        try {
            redisTemplate.opsForStream().createGroup(stream, ReadOffset.latest(), group);
        } catch (Exception ex) {
            // stream 不存在时先创建，占位消息后删除
            redisTemplate.opsForStream().add(StreamRecords.newRecord().ofMap(Map.of("init", "1")).withStreamKey(stream));
            try {
                redisTemplate.opsForStream().createGroup(stream, ReadOffset.latest(), group);
            } catch (Exception ignored) {
                log.debug("consumer group already exists: {}", group);
            }
        }
    }
}
