package com.interview.aichat.knowledge.rag;

import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PgVectorRetrievalService {

    private final JdbcTemplate jdbcTemplate;
    private final AliyunEmbeddingClient embeddingClient;

    public List<KnowledgeChunk> search(String question, int topK) {
        float[] vector = embeddingClient.embed(question);
        String sql = """
                SELECT id, document_id, content, 1 - (embedding <=> ?) AS score
                FROM kb_chunks
                ORDER BY embedding <=> ?
                LIMIT ?
                """;

        return jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            PGvector pgVector = new PGvector(vector);
            ps.setObject(1, pgVector);
            ps.setObject(2, pgVector);
            ps.setInt(3, topK);
            return ps;
        }, (rs, i) -> KnowledgeChunk.builder()
                .id(rs.getLong("id"))
                .documentId(rs.getString("document_id"))
                .content(rs.getString("content"))
                .score(rs.getDouble("score"))
                .build());
    }
}
