package com.interview.aichat.knowledge.ingest;

import com.interview.aichat.file.DocumentParseService;
import com.interview.aichat.knowledge.chunk.DocumentChunker;
import com.interview.aichat.knowledge.dto.KnowledgeBaseUploadResponse;
import com.interview.aichat.knowledge.rag.AliyunEmbeddingClient;
import com.interview.kevin.exception.BusinessException;
import com.interview.kevin.exception.ErrorCode;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseIngestionService {

    private final DocumentParseService documentParseService;
    private final JdbcTemplate jdbcTemplate;
    private final AliyunEmbeddingClient embeddingClient;
    private final DocumentChunker documentChunker;

    @Transactional
    public KnowledgeBaseUploadResponse ingest(
            MultipartFile file,
            String documentId,
            boolean override,
            int chunkSizeChars,
            int chunkOverlapChars
    ) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "uploaded file is empty");
        }

        String content;
        try {
            content = documentParseService.parseContent(file);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_PARSE_FAILED, "parse content failed: " + e.getMessage());
        }

        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_PARSE_FAILED, "parsed content is empty");
        }

        String docId = normalizeOrGenerateDocumentId(documentId, file.getOriginalFilename());
        if (override) {
            jdbcTemplate.update("DELETE FROM kb_chunks WHERE document_id = ?", docId);
        }

        List<String> chunks = documentChunker.chunk(content, chunkSizeChars, chunkOverlapChars)
                .stream()
                // avoid embedding extremely small chunks
                .filter(c -> c.length() >= 50)
                .toList();

        if (chunks.isEmpty()) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_UPLOAD_FAILED, "no chunks generated");
        }

        String insertSql = """
                INSERT INTO kb_chunks(document_id, content, embedding)
                VALUES (?, ?, ?)
                """;

        // Note: embedding call is the bottleneck; we batch DB writes but still compute embeddings per chunk.
        List<float[]> embeddings = new ArrayList<>(chunks.size());
        try {
            for (String chunk : chunks) {
                embeddings.add(embeddingClient.embed(chunk));
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_VECTORIZATION_FAILED, "embedding failed: " + e.getMessage());
        }

        try {
            jdbcTemplate.batchUpdate(
                    insertSql,
                    new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(@org.springframework.lang.NonNull java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                            ps.setString(1, docId);
                            ps.setString(2, chunks.get(i));
                            ps.setObject(3, new PGvector(embeddings.get(i)));
                        }

                        @Override
                        public int getBatchSize() {
                            return chunks.size();
                        }
                    }
            );
        } catch (Exception e) {
            log.error("kb_chunks insert failed, docId={}, err={}", docId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_UPLOAD_FAILED, "insert kb_chunks failed: " + e.getMessage());
        }

        return KnowledgeBaseUploadResponse.builder()
                .documentId(docId)
                .chunks(chunks.size())
                .overridden(override)
                .build();
    }

    private String normalizeOrGenerateDocumentId(String documentId, String originalFilename) {
        if (documentId != null && !documentId.isBlank()) {
            return truncateToMaxLen(sanitizeDocumentId(documentId), 128);
        }

        String name = originalFilename == null ? "unknown" : originalFilename;
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        String safeName = sanitizeDocumentId(name);
        if (safeName.isBlank()) {
            return UUID.randomUUID().toString().substring(0, 8);
        }
        return truncateToMaxLen(safeName, 128);
    }

    private String sanitizeDocumentId(String raw) {
        String v = Objects.requireNonNullElse(raw, "");
        // keep it simple: letters/digits/underscore/dash/dot
        return v.replaceAll("[^a-zA-Z0-9._-]", "_").strip();
    }

    private String truncateToMaxLen(String v, int maxLen) {
        if (v == null) {
            return null;
        }
        if (v.length() <= maxLen) {
            return v;
        }
        return v.substring(0, maxLen);
    }
}

