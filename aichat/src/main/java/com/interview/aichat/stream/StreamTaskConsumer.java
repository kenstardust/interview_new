package com.interview.aichat.stream;

import com.interview.aichat.file.DocumentParseService;
import com.interview.aichat.file.FileStorageService;
import com.interview.kevin.constant.TaskStatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamTaskConsumer {

    private final StringRedisTemplate redisTemplate;
    private final RedisStreamProperties streamProperties;
    private final TaskStatusService taskStatusService;
    private final FileStorageService fileStorageService;
    private final DocumentParseService documentParseService;

    @Qualifier("streamBusinessExecutor")
    private final Executor streamBusinessExecutor;

    private String consumerName;

    @EventListener(ApplicationReadyEvent.class)
    public void initConsumerGroup() {
        consumerName = buildConsumerName();
        try {
            if (Boolean.FALSE.equals(redisTemplate.hasKey(streamProperties.getKey()))) {
                redisTemplate.opsForStream().add(streamProperties.getKey(), Map.of("bootstrap", "1"));
            }
            redisTemplate.opsForStream()
                    .createGroup(streamProperties.getKey(), ReadOffset.latest(), streamProperties.getGroup());
            log.info("创建 Stream Group 成功: stream={}, group={}", streamProperties.getKey(), streamProperties.getGroup());
        } catch (Exception e) {
            log.info("Stream Group 已存在或创建失败(可忽略): {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void layerOnePullAndDispatch() {
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                Consumer.from(streamProperties.getGroup(), consumerName),
                StreamReadOptions.empty().count(10).block(Duration.ofSeconds(1)),
                StreamOffset.create(streamProperties.getKey(), ReadOffset.lastConsumed())
        );

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            streamBusinessExecutor.execute(() -> layerTwoProcess(record));
        }
    }

    private void layerTwoProcess(MapRecord<String, Object, Object> record) {
        String taskId = get(record, "taskId");
        String storageKey = get(record, "storageKey");
        String fileName = get(record, "fileName");

        try {
            taskStatusService.update(taskId, TaskStatusCode.PROCESSING);

            String content = documentParseService.downloadAndParseContent(fileStorageService, storageKey, fileName);
            int contentLength = content == null ? 0 : content.length();

            taskStatusService.update(taskId, TaskStatusCode.COMPLETED, "contentLength=" + contentLength);
            ack(record.getId());
        } catch (Exception ex) {
            log.error("消费任务失败 taskId={}, err={}", taskId, ex.getMessage(), ex);
            taskStatusService.update(taskId, TaskStatusCode.FAILED, ex.getMessage());
            ack(record.getId());
        }
    }

    /**
     * 扫描 PEL（Pending Entries List）并把超时消息重新拉起处理。
     */
    @Scheduled(initialDelay = 15000, fixedDelay = 15000)
    public void retryPendingMessage() {
        PendingMessagesSummary summary = redisTemplate.opsForStream().pending(
                streamProperties.getKey(), streamProperties.getGroup()
        );

        if (summary == null || summary.getTotalPendingMessages() == 0) {
            return;
        }

        PendingMessages pendingMessages = redisTemplate.opsForStream().pending(
                streamProperties.getKey(),
                Consumer.from(streamProperties.getGroup(), consumerName),
                Range.unbounded(),
                20
        );

        for (PendingMessage pendingMessage : pendingMessages) {
            if (pendingMessage.getElapsedTimeSinceLastDelivery().toMillis() < streamProperties.getPendingIdleMs()) {
                continue;
            }

            List<MapRecord<String, Object, Object>> claimed = redisTemplate.opsForStream().claim(
                    streamProperties.getKey(),
                    streamProperties.getGroup(),
                    consumerName,
                    Duration.ofMillis(streamProperties.getPendingIdleMs()),
                    pendingMessage.getId()
            );

            if (claimed != null) {
                for (MapRecord<String, Object, Object> record : claimed) {
                    streamBusinessExecutor.execute(() -> layerTwoProcess(record));
                }
            }
        }
    }

    private void ack(RecordId recordId) {
        redisTemplate.opsForStream().acknowledge(streamProperties.getKey(), streamProperties.getGroup(), recordId);
    }

    private String get(MapRecord<String, Object, Object> record, String key) {
        Object value = record.getValue().get(key);
        return value == null ? null : value.toString();
    }

    private String buildConsumerName() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID();
        } catch (UnknownHostException e) {
            return "consumer-" + UUID.randomUUID();
        }
    }
}
