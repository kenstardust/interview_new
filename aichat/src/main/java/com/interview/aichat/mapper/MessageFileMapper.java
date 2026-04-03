package com.interview.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.aichat.model.MessageFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 消息-文件关联Mapper：提供消息与文件关联的查询方法
 *
 * 继承MyBatis-Plus的BaseMapper，自动提供基础CRUD方法
 */
@Mapper
public interface MessageFileMapper extends BaseMapper<MessageFile> {

    /**
     * 根据消息ID查询关联的文件
     *
     * @param messageId 消息ID
     * @return 关联列表
     */
    @Select("SELECT * FROM message_file WHERE message_id = #{messageId}")
    List<MessageFile> findByMessageId(@Param("messageId") Long messageId);

    /**
     * 根据文件ID查询关联的消息
     *
     * @param fileId 文件ID
     * @return 关联列表
     */
    @Select("SELECT * FROM message_file WHERE file_id = #{fileId}")
    List<MessageFile> findByFileId(@Param("fileId") Long fileId);

    /**
     * 查询消息关联的文件ID列表
     *
     * @param messageId 消息ID
     * @return 文件ID列表
     */
    @Select("SELECT file_id FROM message_file WHERE message_id = #{messageId}")
    List<Long> findFileIdsByMessageId(@Param("messageId") Long messageId);

    /**
     * 统计文件的引用次数（被多少条消息引用）
     *
     * @param fileId 文件ID
     * @return 引用次数
     */
    @Select("SELECT COUNT(*) FROM message_file WHERE file_id = #{fileId}")
    Integer countByFileId(@Param("fileId") Long fileId);

    /**
     * 删除消息的所有文件关联
     *
     * @param messageId 消息ID
     * @return 删除数量
     */
    @Select("DELETE FROM message_file WHERE message_id = #{messageId}")
    Integer deleteByMessageId(@Param("messageId") Long messageId);
}