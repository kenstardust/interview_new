package com.interview.aichat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.interview.aichat.model.ChatConversation;

import java.util.List;

/**
 * 会话Service：管理会话的创建、查询、删除等操作
 *
 * 继承MyBatis-Plus的IService，自动提供基础CRUD方法
 */
public interface ChatConversationService extends IService<ChatConversation> {

    /**
     * 创建新会话
     *
     * @param title 会话标题（可选，如果不提供会自动生成）
     * @return 创建的会话对象
     */
    ChatConversation createConversation(String title);

    /**
     * 根据conversationId查询会话
     *
     * @param conversationId 会话UUID
     * @return 会话对象，如果不存在返回null
     */
    ChatConversation getConversation(String conversationId);

    /**
     * 分页查询会话列表（按更新时间倒序）
     *
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return 会话列表
     */
    List<ChatConversation> listConversations(int page, int size);

    /**
     * 删除会话（软删除，设置status=3）
     *
     * @param conversationId 会话UUID
     */
    void deleteConversation(String conversationId);

    /**
     * 更新会话标题
     *
     * @param conversationId 会话UUID
     * @param title          新标题
     */
    void updateConversationTitle(String conversationId, String title);

    /**
     * 归档会话（设置status=2）
     *
     * @param conversationId 会话UUID
     */
    void archiveConversation(String conversationId);

    /**
     * 统计活跃会话总数
     *
     * @return 总数
     */
    Integer countActiveConversations();
}