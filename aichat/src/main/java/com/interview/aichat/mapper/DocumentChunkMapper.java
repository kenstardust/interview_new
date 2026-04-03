package com.interview.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.aichat.model.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文档分块Mapper：RAG核心Mapper，提供向量相似度检索功能
 *
 * 继承MyBatis-Plus的BaseMapper，自动提供基础CRUD方法
 *
 * 核心功能：
 * 1. 向量相似度检索（findSimilarChunks）- 使用pgvector的余弦距离操作符 <=>）
 * 2. 文档分块查询（findByFileId）
 */
@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {

    /**
     * 向量相似度检索：查询最相似的Top-K文档分块
     *
     * 使用pgvector的余弦距离操作符 <=> 进行相似度计算
     *
     * SQL说明：
     * - embedding <=> #{embedding}::vector：计算余弦距离（越小越相似）
     * - ORDER BY ... LIMIT #{limit}：返回最相似的Top-K结果
     *
     * 注意：
     * - 只查询基本字段（id, file_id, chunk_id, content, chunk_index），不返回embedding向量（节省内存）
     * - 使用HNSW索引加速检索（见V1__Create_RAG_Tables.sql）
     *
     * @param embedding 查询向量（1536维float[]数组）
     * @param limit     返回数量（Top-K，推荐5-10）
     * @return 相似文档分块列表（按相似度排序）
     */
    @Select("<script>" +
            "SELECT id, file_id, chunk_id, content, chunk_index, start_position, end_position, token_count, created_at " +
            "FROM document_chunk " +
            "ORDER BY embedding &lt;=&gt; #{embedding}::vector " +
            "LIMIT #{limit}" +
            "</script>")
    List<DocumentChunk> findSimilarChunks(@Param("embedding") float[] embedding, @Param("limit") int limit);

    /**
     * 向量相似度检索（限定文件范围）：在指定文件中查询相似分块
     *
     * 用途：用户发送消息时指定了fileIds，限定检索范围
     *
     * @param embedding  查询向量
     * @param fileIds    文件ID列表
     * @param limit      返回数量
     * @return 相似文档分块列表
     */
    @Select("<script>" +
            "SELECT id, file_id, chunk_id, content, chunk_index, start_position, end_position, token_count, created_at " +
            "FROM document_chunk " +
            "WHERE file_id IN " +
            "<foreach item='fileId' collection='fileIds' open='(' separator=',' close=')'>" +
            "#{fileId}" +
            "</foreach>" +
            "ORDER BY embedding &lt;=&gt; #{embedding}::vector " +
            "LIMIT #{limit}" +
            "</script>")
    List<DocumentChunk> findSimilarChunksInFiles(@Param("embedding") float[] embedding,
                                                  @Param("fileIds") List<Long> fileIds,
                                                  @Param("limit") int limit);

    /**
     * 根据文件ID查询所有分块（按chunk_index正序）
     *
     * 用途：
     * 1. 查看文档的分块详情
     * 2. 重新生成向量嵌入
     * 3. 删除文档时批量删除分块
     *
     * @param fileId 文件ID
     * @return 分块列表（按序号排序）
     */
    @Select("SELECT id, file_id, chunk_id, content, chunk_index, start_position, end_position, token_count, created_at " +
            "FROM document_chunk " +
            "WHERE file_id = #{fileId} " +
            "ORDER BY chunk_index ASC")
    List<DocumentChunk> findByFileId(@Param("fileId") Long fileId);

    /**
     * 统计文件的分块数量
     *
     * @param fileId 文件ID
     * @return 分块数量
     */
    @Select("SELECT COUNT(*) FROM document_chunk WHERE file_id = #{fileId}")
    Integer countByFileId(@Param("fileId") Long fileId);

    /**
     * 查询文件的分块（包含向量嵌入）
     *
     * 注意：此方法会返回embedding字段，占用较多内存，谨慎使用
     *
     * @param fileId 文件ID
     * @return 分块列表（包含向量）
     */
    @Select("SELECT * FROM document_chunk WHERE file_id = #{fileId} ORDER BY chunk_index ASC")
    List<DocumentChunk> findByFileIdWithEmbedding(@Param("fileId") Long fileId);

    /**
     * 根据chunkId查询分块
     *
     * @param chunkId 分块UUID
     * @return 分块对象
     */
    @Select("SELECT * FROM document_chunk WHERE chunk_id = #{chunkId}")
    DocumentChunk findByChunkId(@Param("chunkId") String chunkId);

    /**
     * 删除文件的所有分块
     *
     * @param fileId 文件ID
     * @return 删除数量
     */
    @Select("DELETE FROM document_chunk WHERE file_id = #{fileId}")
    Integer deleteByFileId(@Param("fileId") Long fileId);
}