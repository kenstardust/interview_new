package com.interview.aichat.service.rag;

import com.interview.aichat.mapper.DocumentChunkMapper;
import com.interview.aichat.model.DocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 检索Service：从向量数据库中检索相关文档分块
 *
 * 核心功能：
 * 1. 向量相似度检索：retrieveRelevantChunks(String query, int topK)
 * 2. 构建上下文字符串：buildContextString(List<DocumentChunk> chunks)
 *
 * 检索流程：
 * 1. 用户提问 → 生成查询向量
 * 2. pgvector相似度检索（余弦距离）
 * 3. 返回Top-K最相关分块
 * 4. 构建上下文Prompt
 */
@Slf4j
@Service
public class RetrievalService {

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private DocumentChunkMapper documentChunkMapper;

    /**
     * 默认检索数量（Top-K）
     */
    private static final int DEFAULT_TOP_K = 5;

    /**
     * 最大检索数量
     */
    private static final int MAX_TOP_K = 10;

    /**
     * 上下文最大长度（字符数）
     * 避免超出LLM token限制
     */
    private static final int MAX_CONTEXT_LENGTH = 8000;  // 约2000 tokens

    /**
     * 检索相关文档分块
     *
     * @param query 用户问题
     * @param topK  返回数量（默认5）
     * @return 相关文档分块列表（按相似度排序）
     */
    public List<DocumentChunk> retrieveRelevantChunks(String query, int topK) {
        if (query == null || query.trim().isEmpty()) {
            log.warn("查询为空，返回空列表");
            return List.of();
        }

        // 限制topK范围
        if (topK < 1) {
            topK = DEFAULT_TOP_K;
        } else if (topK > MAX_TOP_K) {
            topK = MAX_TOP_K;
        }

        log.info("开始检索：query={}, topK={}", query.substring(0, Math.min(50, query.length())), topK);

        try {
            // 1. 生成查询向量
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            if (queryEmbedding == null) {
                log.error("查询向量生成失败");
                return List.of();
            }

            // 2. pgvector相似度检索
            List<DocumentChunk> chunks = documentChunkMapper.findSimilarChunks(queryEmbedding, topK);

            log.info("检索完成：找到{}个相关分块", chunks.size());
            return chunks;

        } catch (Exception e) {
            log.error("检索失败：query={}, error={}", query, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 检索相关文档分块（限定文件范围）
     *
     * 用途：用户发送消息时指定了fileIds，限定检索范围
     *
     * @param query   用户问题
     * @param fileIds 文件ID列表
     * @param topK    返回数量
     * @return 相关文档分块列表
     */
    public List<DocumentChunk> retrieveRelevantChunksInFiles(String query, List<Long> fileIds, int topK) {
        if (query == null || query.trim().isEmpty()) {
            log.warn("查询为空，返回空列表");
            return List.of();
        }
        if (fileIds == null || fileIds.isEmpty()) {
            log.warn("文件ID列表为空，使用全局检索");
            return retrieveRelevantChunks(query, topK);
        }

        // 限制topK范围
        if (topK < 1) {
            topK = DEFAULT_TOP_K;
        } else if (topK > MAX_TOP_K) {
            topK = MAX_TOP_K;
        }

        log.info("开始检索（限定文件）：query={}, fileIds={}, topK={}",
                query.substring(0, Math.min(50, query.length())), fileIds, topK);

        try {
            // 1. 生成查询向量
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            if (queryEmbedding == null) {
                log.error("查询向量生成失败");
                return List.of();
            }

            // 2. pgvector相似度检索（限定文件范围）
            List<DocumentChunk> chunks = documentChunkMapper.findSimilarChunksInFiles(queryEmbedding, fileIds, topK);

            log.info("检索完成（限定文件）：找到{}个相关分块", chunks.size());
            return chunks;

        } catch (Exception e) {
            log.error("检索失败：query={}, error={}", query, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 构建上下文字符串
     *
     * 将检索到的分块格式化为Prompt上下文
     * 格式：
     * [文档1 - 分块1]
     * 内容...
     *
     * [文档2 - 分块1]
     * 内容...
     *
     * @param chunks 检索到的分块列表
     * @return 格式化的上下文字符串
     */
    public String buildContextString(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "（未找到相关上下文）";
        }

        StringBuilder context = new StringBuilder();
        int currentLength = 0;

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);

            // 格式：[文档ID:chunkId]
            String chunkHeader = String.format("\n[文档%d - 分块%d]\n", i + 1, chunk.getChunkIndex() + 1);
            String chunkContent = chunk.getContent();

            // 检查长度限制
            if (currentLength + chunkHeader.length() + chunkContent.length() > MAX_CONTEXT_LENGTH) {
                log.warn("上下文达到最大长度限制，截断后续分块");
                break;
            }

            context.append(chunkHeader);
            context.append(chunkContent);
            context.append("\n");

            currentLength += chunkHeader.length() + chunkContent.length();
        }

        return context.toString();
    }

    /**
     * 构建来源引用字符串
     *
     * 用于在回答中标注来源
     *
     * @param chunks 分块列表
     * @return 来源引用字符串
     */
    public String buildSourceReferences(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }

        return chunks.stream()
                .map(chunk -> String.format("[文档%d-分块%d]",
                        chunk.getFileId(), chunk.getChunkIndex() + 1))
                .collect(Collectors.joining(", "));
    }

    /**
     * 计算上下文总Token数
     *
     * @param chunks 分块列表
     * @return 总Token数
     */
    public int calculateTotalTokens(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return 0;
        }

        return chunks.stream()
                .mapToInt(chunk -> chunk.getTokenCount() != null ? chunk.getTokenCount() : 0)
                .sum();
    }
}