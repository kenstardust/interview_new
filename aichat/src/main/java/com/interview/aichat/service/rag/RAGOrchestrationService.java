package com.interview.aichat.service.rag;

import com.interview.aichat.mapper.DocumentChunkMapper;
import com.interview.aichat.model.ChatFile;
import com.interview.aichat.model.DocumentChunk;
import com.interview.aichat.service.ChatFileService;
import com.interview.kevin.exception.BusinessException;
import com.interview.kevin.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * RAG编排Service：协调整个RAG流程
 *
 * 核心功能：
 * 1. 文档处理流水线：processAndIndexDocument(ChatFile file)
 * 2. 异步处理：processAndIndexDocumentAsync(ChatFile file)
 * 3. RAG问答生成：generateRAGResponse(String conversationId, String query, List<Long> fileIds)
 *
 * RAG流程：
 * 1. 文档上传 → 解析 → 分块 → 向量化 → 存储
 * 2. 用户提问 → 向量检索 → 构建Prompt → LLM生成 → 返回答案
 */
@Slf4j
@Service
public class RAGOrchestrationService {

    @Autowired
    private ChatFileService chatFileService;

    @Autowired
    private DocumentChunkingService chunkingService;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private RetrievalService retrievalService;

    @Autowired
    private DocumentChunkMapper documentChunkMapper;

    /**
     * 异步处理并索引文档
     *
     * 流程：
     * 1. 更新文件状态为PROCESSING
     * 2. 文档分块
     * 3. 批量生成向量
     * 4. 存储向量到数据库
     * 5. 更新文件状态为COMPLETED
     *
     * @param file 文件对象
     * @return 异步结果
     */
    @Async("ragTaskExecutor")
    public CompletableFuture<Void> processAndIndexDocumentAsync(ChatFile file) {
        log.info("开始异步处理文档：fileId={}, filename={}", file.getId(), file.getOriginalfilename());

        try {
            processAndIndexDocument(file);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("文档处理失败：fileId={}, error={}", file.getId(), e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 同步处理并索引文档
     *
     * @param file 文件对象
     */
    public void processAndIndexDocument(ChatFile file) {
        Long fileId = file.getId();

        try {
            // 1. 更新文件状态为PROCESSING
            updateFileStatus(fileId, "PROCESSING", null);

            // 2. 检查文档内容
            String content = file.getFiletext();
            if (content == null || content.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_PARSE_FAILED, "文档内容为空");
            }

            log.info("文档分块开始：fileId={}, contentLength={}", fileId, content.length());

            // 3. 文档分块
            List<DocumentChunk> chunks = chunkingService.chunkDocument(file, content);
            if (chunks.isEmpty()) {
                throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_PARSE_FAILED, "文档分块失败");
            }

            log.info("向量生成开始：fileId={}, chunkCount={}", fileId, chunks.size());

            // 4. 批量生成向量嵌入
            List<String> chunkContents = chunks.stream()
                    .map(DocumentChunk::getContent)
                    .collect(Collectors.toList());

            List<float[]> embeddings = embeddingService.batchGenerateEmbeddings(chunkContents);

            log.info("向量存储开始：fileId={}, embeddingCount={}", fileId, embeddings.size());

            // 5. 存储分块和向量到数据库
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = chunks.get(i);
                chunk.setEmbedding(embeddings.get(i));
                documentChunkMapper.insert(chunk);
            }

            // 6. 更新文件状态为COMPLETED
            updateFileStatus(fileId, "COMPLETED", null);

            log.info("文档处理完成：fileId={}, chunkCount={}", fileId, chunks.size());

        } catch (BusinessException e) {
            // 业务异常：更新状态为FAILED
            updateFileStatus(fileId, "FAILED", e.getMessage());
            throw e;
        } catch (Exception e) {
            // 其他异常：更新状态为FAILED
            String errorMessage = "文档处理失败：" + e.getMessage();
            updateFileStatus(fileId, "FAILED", errorMessage);
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_VECTORIZATION_FAILED, errorMessage);
        }
    }

    /**
     * 生成RAG回答
     *
     * 流程：
     * 1. 检索相关文档分块
     * 2. 构建上下文
     * 3. 构建Prompt
     * 4. 调用LLM生成回答
     *
     * @param conversationId 会话ID
     * @param query          用户问题
     * @param fileIds        文件ID列表（可选，用于限定检索范围）
     * @return AI回答
     */
    public String generateRAGResponse(String conversationId, String query, List<Long> fileIds) {
        log.info("生成RAG回答：conversationId={}, query={}", conversationId,
                query.substring(0, Math.min(50, query.length())));

        try {
            // 1. 检索相关文档分块
            List<DocumentChunk> relevantChunks;
            if (fileIds != null && !fileIds.isEmpty()) {
                // 限定文件范围检索
                relevantChunks = retrievalService.retrieveRelevantChunksInFiles(query, fileIds, 5);
            } else {
                // 全局检索
                relevantChunks = retrievalService.retrieveRelevantChunks(query, 5);
            }

            // 2. 构建上下文
            String context = retrievalService.buildContextString(relevantChunks);

            // 3. 构建RAG Prompt
            String ragPrompt = buildRAGPrompt(context, query);

            log.info("RAG Prompt构建完成：contextLength={}, promptLength={}",
                    context.length(), ragPrompt.length());

            // TODO: 4. 调用LLM生成回答
            // ChatResponse response = chatModel.call(ragPrompt);
            // String answer = response.getResult().getOutput().getContent();

            // 目前返回占位回答
            String answer = buildPlaceholderAnswer(query, relevantChunks);

            log.info("RAG回答生成完成：answerLength={}", answer.length());
            return answer;

        } catch (Exception e) {
            log.error("RAG回答生成失败：conversationId={}, error={}", conversationId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "RAG回答生成失败：" + e.getMessage());
        }
    }

    /**
     * 构建RAG Prompt
     *
     * @param context 上下文
     * @param query   用户问题
     * @return 完整Prompt
     */
    private String buildRAGPrompt(String context, String query) {
        return String.format("""
                你是企业知识库助手，请基于以下上下文回答用户问题。

                上下文：
                %s

                用户问题：%s

                回答要求：
                1. 仅基于上下文信息回答，不编造信息
                2. 如果上下文不包含相关信息，明确说明
                3. 回答简洁清晰，控制在500字以内
                4. 标注信息来源（如：[文档1-分块2]）

                请回答：
                """, context, query);
    }

    /**
     * 构建占位回答（测试用）
     *
     * @param query           用户问题
     * @param relevantChunks  相关分块
     * @return 占位回答
     */
    private String buildPlaceholderAnswer(String query, List<DocumentChunk> relevantChunks) {
        StringBuilder answer = new StringBuilder();
        answer.append("收到您的问题：").append(query).append("\n\n");

        if (relevantChunks.isEmpty()) {
            answer.append("抱歉，未在知识库中找到相关信息。\n\n");
        } else {
            answer.append("找到").append(relevantChunks.size()).append("个相关文档片段：\n\n");

            for (int i = 0; i < relevantChunks.size(); i++) {
                DocumentChunk chunk = relevantChunks.get(i);
                answer.append(String.format("[文档%d - 分块%d]\n", i + 1, chunk.getChunkIndex() + 1));
                answer.append(chunk.getContent().substring(0, Math.min(100, chunk.getContent().length())));
                answer.append("...\n\n");
            }
        }

        answer.append("\n（注：系统正在建设中，此为占位回答，实际回答将由AI生成）");
        return answer.toString();
    }

    /**
     * 更新文件状态
     *
     * @param fileId   文件ID
     * @param status   状态（PENDING, PROCESSING, COMPLETED, FAILED）
     * @param errorMsg 错误信息（可选）
     */
    private void updateFileStatus(Long fileId, String status, String errorMsg) {
        try {
            ChatFile file = chatFileService.getById(fileId);
            if (file != null) {
                file.setTaskstatus(status);
                if (errorMsg != null) {
                    file.setAnalyzeError(errorMsg);
                }
                chatFileService.updateById(file);
                log.info("文件状态已更新：fileId={}, status={}", fileId, status);
            }
        } catch (Exception e) {
            log.error("更新文件状态失败：fileId={}, error={}", fileId, e.getMessage());
        }
    }

    /**
     * 删除文档的所有向量索引
     *
     * @param fileId 文件ID
     */
    public void deleteDocumentIndex(Long fileId) {
        log.info("删除文档索引：fileId={}", fileId);

        try {
            // 删除document_chunk表中的所有分块
            Integer deletedCount = documentChunkMapper.deleteByFileId(fileId);

            log.info("文档索引删除完成：fileId={}, deletedCount={}", fileId, deletedCount);

        } catch (Exception e) {
            log.error("删除文档索引失败：fileId={}, error={}", fileId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_DELETE_FAILED, "删除文档索引失败");
        }
    }

    /**
     * 统计文件的分块数量
     *
     * @param fileId 文件ID
     * @return 分块数量
     */
    public Integer getDocumentChunkCount(Long fileId) {
        return documentChunkMapper.countByFileId(fileId);
    }
}