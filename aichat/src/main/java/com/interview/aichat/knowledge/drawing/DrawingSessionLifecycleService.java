package com.interview.aichat.knowledge.drawing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DrawingSessionLifecycleService {

    private static final Duration HOT_CACHE_TTL = Duration.ofMinutes(30);
    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void updateState(String sessionId, DrawingSessionState state, String answerDraft) {
        DrawingSessionSnapshot snapshot = DrawingSessionSnapshot.builder()
                .sessionId(sessionId)
                .state(state)
                .answerDraft(answerDraft)
                .updatedAt(Instant.now())
                .build();
        cacheHotSnapshot(snapshot);
        persistColdSnapshot(snapshot);
    }

    public DrawingSessionSnapshot resume(String sessionId) {
        String key = redisKey(sessionId);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, DrawingSessionSnapshot.class);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("恢复绘画会话失败", e);
            }
        }

        return jdbcTemplate.queryForObject(
                "SELECT session_id, state, answer_draft, updated_at FROM drawing_session WHERE session_id = ?",
                (rs, i) -> DrawingSessionSnapshot.builder()
                        .sessionId(rs.getString("session_id"))
                        .state(DrawingSessionState.valueOf(rs.getString("state")))
                        .answerDraft(rs.getString("answer_draft"))
                        .updatedAt(rs.getTimestamp("updated_at").toInstant())
                        .build(),
                sessionId
        );
    }

    private void cacheHotSnapshot(DrawingSessionSnapshot snapshot) {
        try {
            redisTemplate.opsForValue().set(
                    redisKey(snapshot.getSessionId()),
                    objectMapper.writeValueAsString(snapshot),
                    HOT_CACHE_TTL
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("绘画会话缓存失败", e);
        }
    }

    private void persistColdSnapshot(DrawingSessionSnapshot snapshot) {
        jdbcTemplate.update("""
                        INSERT INTO drawing_session(session_id, state, answer_draft, updated_at)
                        VALUES(?, ?, ?, ?)
                        ON CONFLICT (session_id)
                        DO UPDATE SET state = EXCLUDED.state,
                                      answer_draft = EXCLUDED.answer_draft,
                                      updated_at = EXCLUDED.updated_at
                        """,
                snapshot.getSessionId(),
                snapshot.getState().name(),
                snapshot.getAnswerDraft(),
                java.sql.Timestamp.from(snapshot.getUpdatedAt())
        );
    }

    private String redisKey(String sessionId) {
        return "kb:drawing:" + sessionId;
    }
}
