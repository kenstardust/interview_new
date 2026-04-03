package com.interview.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.aichat.model.ChatConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 会话Mapper：提供会话表的基础CRUD和自定义查询
 *
 * 继承MyBatis-Plus的BaseMapper，自动提供：
 * - insert()
 * - deleteById()
 * - updateById()
 * - selectById()
 * - selectList()
 * 等方法
 */
@Mapper
public interface ChatConversationMapper extends BaseMapper<ChatConversation> {

    /**
     * 根据conversationId查询会话
     *
     * @param conversationId 会话UUID
     * @return 会话对象
     */
    @Select("SELECT * FROM chat_conversation WHERE conversation_id = #{conversationId}")
    ChatConversation findByConversationId(@Param("conversationId") String conversationId);

    /**
     * 查询所有活跃会话（按更新时间倒序）
     *
     * @param status 会话状态（1=ACTIVE）
     * @return 会话列表
     */
    @Select("SELECT * FROM chat_conversation WHERE status = #{status} ORDER BY updated_at DESC")
    List<ChatConversation> findActiveConversations(@Param("status") Integer status);

    /**
     * 分页查询会话列表（按更新时间倒序）
     *
     * @param offset 偏移量（page * size）
     * @param limit  每页数量
     * @return 会话列表
     */
    @Select("SELECT * FROM chat_conversation ORDER BY updated_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<ChatConversation> findConversationsPage(@Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * 统计会话总数
     *
     * @return 总数
     */
    @Select("SELECT COUNT(*) FROM chat_conversation WHERE status = 1")
    Integer countActiveConversations();
}