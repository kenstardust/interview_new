package com.interview.aichat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.interview.aichat.model.ChatMessage;

import java.util.List;

/**
 * 消息Service：管理消息的创建、查询、删除等操作
 *
 * 继承MyBatis-Plus的IService，自动提供基础CRUD方法
 */
public interface ChatMessageService extends IService<ChatMessage> {

    /**
     * 添加消息到会话
     *
     * @param conversationId 会话UUID
     * @param role           消息角色（user/assistant/system）
     * @param content        消息内容
     * @return 创建的消息对象
     */
    ChatMessage addMessage(String conversationId, String role, String content);

    /**
     * 添加消息到会话（包含元数据）
     *
     * @param conversationId 会话UUID
     * @param role           消息角色
     * @param content        消息内容
     * @param metadata       元数据（JSON格式）
     * @return 创建的消息对象
     */
    ChatMessage addMessage(String conversationId, String role, String content, String metadata);

    /**
     * 根据会话ID查询所有消息
     *
     * @param conversationId 会话UUID
     * @return 消息列表（按创建时间正序）
     */
    List<ChatMessage> getConversationMessages(String conversationId);

    /**
     * 根据会话ID查询最近的N条消息
     *
     * @param conversationId 会话UUID
     * @param limit          数量限制
     * @return 消息列表（按创建时间倒序，最新的在前）
     */
    List<ChatMessage> getRecentMessages(String conversationId, int limit);

    /**
     * 删除消息
     *
     * @param messageId 消息ID
     */
    void deleteMessage(Long messageId);

    /**
     * 统计会话的消息数量
     *
     * @param conversationId 会话UUID
     * @return 消息数量
     */
    Integer countMessages(String conversationId);
}