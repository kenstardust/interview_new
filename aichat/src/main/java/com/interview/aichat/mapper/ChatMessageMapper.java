package com.interview.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.aichat.model.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 消息Mapper：提供消息表的基础CRUD和自定义查询
 *
 * 继承MyBatis-Plus的BaseMapper，自动提供基础CRUD方法
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 根据conversationId查询所有消息（按创建时间正序）
     *
     * @param conversationId 会话UUID
     * @return 消息列表
     */
    @Select("SELECT * FROM chat_message WHERE conversation_id = #{conversationId} ORDER BY created_at ASC")
    List<ChatMessage> findByConversationId(@Param("conversationId") String conversationId);

    /**
     * 根据conversationId查询最近的N条消息
     *
     * @param conversationId 会话UUID
     * @param limit          数量限制
     * @return 消息列表（倒序，最新的在前）
     */
    @Select("SELECT * FROM chat_message WHERE conversation_id = #{conversationId} ORDER BY created_at DESC LIMIT #{limit}")
    List<ChatMessage> findRecentMessages(@Param("conversationId") String conversationId, @Param("limit") Integer limit);

    /**
     * 统计会话的消息数量
     *
     * @param conversationId 会话UUID
     * @return 消息数量
     */
    @Select("SELECT COUNT(*) FROM chat_message WHERE conversation_id = #{conversationId}")
    Integer countByConversationId(@Param("conversationId") String conversationId);

    /**
     * 查询用户消息（role='user'）
     *
     * @param conversationId 会话UUID
     * @return 用户消息列表
     */
    @Select("SELECT * FROM chat_message WHERE conversation_id = #{conversationId} AND role = 'user' ORDER BY created_at ASC")
    List<ChatMessage> findUserMessages(@Param("conversationId") String conversationId);

    /**
     * 查询助手消息（role='assistant'）
     *
     * @param conversationId 会话UUID
     * @return 助手消息列表
     */
    @Select("SELECT * FROM chat_message WHERE conversation_id = #{conversationId} AND role = 'assistant' ORDER BY created_at ASC")
    List<ChatMessage> findAssistantMessages(@Param("conversationId") String conversationId);
}