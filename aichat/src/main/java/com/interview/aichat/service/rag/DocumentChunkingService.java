package com.interview.aichat.service.rag;

import com.interview.aichat.model.ChatFile;
import com.interview.aichat.model.DocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文档分块Service：将长文档分割成适合向量检索的小块
 *
 * 分块策略：
 * 1. 按段落优先分割（保持语义完整性）
 * 2. 固定大小 + 重叠窗口（保持上下文连贯性）
 * 3. 参数：chunkSize=800 tokens (约2000中文字符), overlap=100 tokens (约400字符)
 *
 * 为什么需要分块？
 * - LLM有token限制，无法处理超长文档
 * - 向量检索精度：小块更精准
 * - 成本控制：减少无用token消耗
 */
@Slf4j
@Service
public class DocumentChunkingService {

    /**
     * 分块大小（字符数）
     * 约800 tokens，假设4字符=1token，则约3200字符
     * 实际使用2000中文字符（保守估计）
     */
    private static final int CHUNK_SIZE = 2000;

    /**
     * 重叠大小（字符数）
     * 约100 tokens，约400字符
     * 重叠确保跨块上下文不丢失
     */
    private static final int OVERLAP_SIZE = 400;

    /**
     * 最小分块大小
     * 避免生成过小的分块
     */
    private static final int MIN_CHUNK_SIZE = 200;

    /**
     * 将文档分块
     *
     * @param file    文件对象
     * @param content 文档文本内容
     * @return 分块列表
     */
    public List<DocumentChunk> chunkDocument(ChatFile file, String content) {
        if (content == null || content.trim().isEmpty()) {
            log.warn("文档内容为空：fileId={}", file.getId());
            return new ArrayList<>();
        }

        log.info("开始分块文档：fileId={}, contentLength={}", file.getId(), content.length());

        List<DocumentChunk> chunks = new ArrayList<>();

        // 策略1：尝试按段落分割
        List<String> paragraphs = splitByParagraphs(content);

        // 策略2：如果段落过大，进一步按固定大小分割
        List<String> finalChunks = new ArrayList<>();
        for (String paragraph : paragraphs) {
            if (paragraph.length() > CHUNK_SIZE) {
                // 大段落：按固定大小+重叠分割
                List<String> subChunks = splitByFixedSize(paragraph, CHUNK_SIZE, OVERLAP_SIZE);
                finalChunks.addAll(subChunks);
            } else if (paragraph.length() >= MIN_CHUNK_SIZE) {
                // 小段落：直接作为一块
                finalChunks.add(paragraph);
            }
            // 过小的段落（<MIN_CHUNK_SIZE）丢弃或合并到上一块
        }

        // 创建DocumentChunk对象
        int currentPosition = 0;
        for (int i = 0; i < finalChunks.size(); i++) {
            String chunkContent = finalChunks.get(i);

            DocumentChunk chunk = DocumentChunk.builder()
                    .fileId(file.getId())
                    .chunkId(UUID.randomUUID().toString())
                    .content(chunkContent)
                    .chunkIndex(i)
                    .startPosition(currentPosition)
                    .endPosition(currentPosition + chunkContent.length())
                    .tokenCount(estimateTokenCount(chunkContent))
                    .build();

            chunk.init();
            chunks.add(chunk);

            currentPosition += chunkContent.length();
        }

        log.info("文档分块完成：fileId={}, chunkCount={}", file.getId(), chunks.size());
        return chunks;
    }

    /**
     * 按段落分割文档
     *
     * 优先级：双换行符 > 单换行符 > 句号
     *
     * @param content 文档内容
     * @return 段落列表
     */
    private List<String> splitByParagraphs(String content) {
        List<String> paragraphs = new ArrayList<>();

        // 1. 先按双换行符分割（\n\n 表示段落）
        String[] doubleNewlineParts = content.split("\\n\\n+");

        for (String part : doubleNewlineParts) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }

            // 如果段落过大（>CHUNK_SIZE * 1.5），进一步按单换行符分割
            if (part.length() > CHUNK_SIZE * 1.5) {
                String[] singleNewlineParts = part.split("\\n+");
                for (String subPart : singleNewlineParts) {
                    subPart = subPart.trim();
                    if (!subPart.isEmpty()) {
                        paragraphs.add(subPart);
                    }
                }
            } else {
                paragraphs.add(part);
            }
        }

        return paragraphs;
    }

    /**
     * 按固定大小+重叠分割文本
     *
     * 滑动窗口策略：
     * - 窗口大小：chunkSize
     * - 滑动步长：chunkSize - overlap
     *
     * @param text      文本
     * @param chunkSize 分块大小
     * @param overlap   重叠大小
     * @return 分块列表
     */
    private List<String> splitByFixedSize(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();

        if (text.length() <= chunkSize) {
            chunks.add(text);
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // 尝试在句子边界分割（优先选择句号、问号、感叹号）
            if (end < text.length()) {
                int lastSentenceEnd = findLastSentenceEnd(text, start, end);
                if (lastSentenceEnd > start + MIN_CHUNK_SIZE) {
                    end = lastSentenceEnd;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // 滑动窗口：下一个起始位置 = 当前结束位置 - 重叠大小
            start = end - overlap;
            if (start <= chunks.get(chunks.size() - 1).length()) {
                // 避免重复，确保start前进
                start = end;
            }
        }

        return chunks;
    }

    /**
     * 查找最后一句的结束位置
     *
     * 在[start, end]范围内查找最后一个句号、问号、感叹号的位置
     *
     * @param text  文本
     * @param start 起始位置
     * @param end   结束位置
     * @return 最后一句的结束位置，如果没有找到返回end
     */
    private int findLastSentenceEnd(String text, int start, int end) {
        for (int i = end - 1; i >= start; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '？' || c == '！' || c == '.' || c == '?' || c == '!') {
                return i + 1;  // 包含句号本身
            }
        }
        return end;  // 没找到句子边界，返回原始end
    }

    /**
     * 估算token数量
     *
     * 简化估算：中文约4字符=1token，英文约1字符=0.25token
     * 混合文本：总字符数 / 4
     *
     * @param text 文本
     * @return 估算的token数量
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() / 4;
    }
}