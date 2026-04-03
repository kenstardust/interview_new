package com.interview.aichat.service.rag;

import com.interview.kevin.exception.BusinessException;
import com.interview.kevin.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.Embedding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 向量嵌入Service：调用DashScope API生成文本的向量表示
 *
 * 核心功能：
 * 1. 单文本向量化：generateEmbedding(String text)
 * 2. 批量向量化：batchGenerateEmbeddings(List<String> texts)
 *
 * 技术细节：
 * - 模型：text-embedding-v2（DashScope标准embedding模型）
 * - 维度：1536（OpenAI兼容）
 * - 批处理：每批最多20个文本（API限流考虑）
 * - 重试机制：失败重试3次
 *
 * 为什么需要向量嵌入？
 * - 将文本转换为数值向量，才能进行相似度计算
 * - pgvector存储向量并支持相似度检索
 */
@Slf4j
@Service
public class EmbeddingService {

    // 注入Spring AI的EmbeddingModel（DashScope自动配置）
    // 注意：需要确保application.yaml中已配置spring.ai.dashscope.api-key

    /**
     * 批处理大小
     * DashScope API限制：每批最多20个文本
     */
    private static final int BATCH_SIZE = 20;

    /**
     * 重试次数
     */
    private static final int MAX_RETRIES = 3;

    /**
     * 重试延迟（毫秒）
     */
    private static final long RETRY_DELAY_MS = 1000;

    /**
     * 异步线程池（用于批量处理）
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    // TODO: 注入EmbeddingModel（Spring AI 1.1.0.0-RC2的DashScope集成）
    // @Autowired
    // private EmbeddingModel embeddingModel;

    /**
     * 生成单个文本的向量嵌入
     *
     * @param text 文本内容
     * @return 向量数组（1536维float[]）
     */
    public float[] generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("文本为空，返回null向量");
            return null;
        }

        log.debug("生成向量嵌入：textLength={}", text.length());

        try {
            // TODO: 实际调用DashScope Embedding API
            // 目前返回占位向量（全0，1536维）
            // 实际实现示例：
            // EmbeddingRequest request = new EmbeddingRequest(
            //     text,
            //     EmbeddingOptions.builder()
            //         .model("text-embedding-v2")
            //         .build()
            // );
            // EmbeddingResponse response = embeddingModel.embed(request);
            // return response.getResults().get(0).getOutput();

            return generatePlaceholderEmbedding();
        } catch (Exception e) {
            log.error("生成向量嵌入失败：textLength={}, error={}", text.length(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "向量生成失败：" + e.getMessage());
        }
    }

    /**
     * 批量生成向量嵌入
     *
     * 分批处理策略：
     * 1. 每批BATCH_SIZE（20）个文本
     * 2. 失败重试MAX_RETRIES次
     * 3. 返回所有向量列表
     *
     * @param texts 文本列表
     * @return 向量列表（每个元素是1536维float[]）
     */
    public List<float[]> batchGenerateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            log.warn("文本列表为空，返回空向量列表");
            return new ArrayList<>();
        }

        log.info("批量生成向量嵌入：textCount={}", texts.size());

        List<float[]> embeddings = new ArrayList<>();

        // 分批处理
        int totalBatches = (int) Math.ceil((double) texts.size() / BATCH_SIZE);
        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            int startIndex = batchIndex * BATCH_SIZE;
            int endIndex = Math.min(startIndex + BATCH_SIZE, texts.size());
            List<String> batch = texts.subList(startIndex, endIndex);

            log.debug("处理批次 {}/{}：文本数量={}", batchIndex + 1, totalBatches, batch.size());

            // 处理一批文本
            List<float[]> batchEmbeddings = processBatchWithRetry(batch);
            embeddings.addAll(batchEmbeddings);

            // 避免API限流：批次间延迟100ms
            if (batchIndex < totalBatches - 1) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("批次延迟被中断");
                }
            }
        }

        log.info("批量向量生成完成：总数={}", embeddings.size());
        return embeddings;
    }

    /**
     * 处理一批文本（带重试机制）
     *
     * @param batch 文本批次
     * @return 向量列表
     */
    private List<float[]> processBatchWithRetry(List<String> batch) {
        List<float[]> embeddings = new ArrayList<>();

        for (String text : batch) {
            int retryCount = 0;
            float[] embedding = null;

            // 重试循环
            while (retryCount < MAX_RETRIES) {
                try {
                    embedding = generateEmbedding(text);
                    break;  // 成功，退出重试循环
                } catch (Exception e) {
                    retryCount++;
                    if (retryCount >= MAX_RETRIES) {
                        log.error("文本向量化失败（已重试{}次）：textLength={}", MAX_RETRIES, text.length());
                        // 失败后使用占位向量
                        embedding = generatePlaceholderEmbedding();
                    } else {
                        log.warn("文本向量化失败，准备重试（{}/{}）：error={}", retryCount, MAX_RETRIES, e.getMessage());
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }

            embeddings.add(embedding);
        }

        return embeddings;
    }

    /**
     * 生成占位向量（全0，1536维）
     *
     * 用途：
     * 1. 测试阶段（未集成真实API）
     * 2. 失败时的fallback
     *
     * @return 占位向量
     */
    private float[] generatePlaceholderEmbedding() {
        return new float[1536];  // 全0向量
    }

    /**
     * 计算两个向量的余弦相似度
     *
     * 公式：cos(θ) = (A·B) / (||A|| * ||B||)
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 相似度（-1到1，越接近1越相似）
     */
    public double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length != vec2.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}