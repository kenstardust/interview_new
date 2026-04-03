package com.interview.aichat.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档分块模型：RAG核心表，存储文档分块及其向量嵌入
 *
 * 对应表：document_chunk
 *
 * 核心功能：
 * 1. 文档分块存储（chunk_index维护顺序）
 * 2. 向量嵌入存储（embedding字段，1536维）
 * 3. 支持pgvector相似度检索
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("document_chunk")
public class DocumentChunk {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件ID（外键关联chatfile表）
     */
    private Long fileId;

    /**
     * 分块UUID（对外引用ID）
     */
    private String chunkId;

    /**
     * 分块内容（文本）
     */
    private String content;

    /**
     * 分块序号（文档内顺序，0-based）
     */
    private Integer chunkIndex;

    /**
     * 在原文档中的起始字符位置
     */
    private Integer startPosition;

    /**
     * 在原文档中的结束字符位置
     */
    private Integer endPosition;

    /**
     * Token计数（约等于chunk长度/4）
     */
    private Integer tokenCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 向量嵌入（1536维，DashScope text-embedding-v2）
     *
     * 使用pgvector的vector类型存储
     * 通过自定义TypeHandler处理float[] <-> PostgreSQL vector转换
     */
    @TableField(typeHandler = com.interview.aichat.config.PgVectorTypeHandler.class)
    private float[] embedding;

    /**
     * 初始化分块
     * 创建时间设置为当前时间
     */
    public void init() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 估算token数量
     * 简单估算：中文约4字符=1token，英文约1字符=0.25token
     *
     * @param content 文本内容
     * @return 估算的token数量
     */
    public static int estimateTokenCount(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }

        // 简化估算：总字符数 / 4（适用于中英文混合）
        return content.length() / 4;
    }
}