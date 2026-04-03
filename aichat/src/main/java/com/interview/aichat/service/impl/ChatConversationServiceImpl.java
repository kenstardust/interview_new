package com.interview.aichat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interview.aichat.mapper.ChatConversationMapper;
import com.interview.aichat.model.ChatConversation;
import com.interview.aichat.service.ChatConversationService;
import com.interview.kevin.exception.BusinessException;
import com.interview.kevin.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 会话Service实现类
 */
@Slf4j
@Service
public class ChatConversationServiceImpl extends ServiceImpl<ChatConversationMapper, ChatConversation>
        implements ChatConversationService {

    @Override
    public ChatConversation createConversation(String title) {
        // 生成会话UUID
        String conversationId = UUID.randomUUID().toString();

        // 如果未提供标题，使用默认标题
        if (title == null || title.trim().isEmpty()) {
            title = "新会话";
        }

        // 创建会话对象
        ChatConversation conversation = ChatConversation.builder()
                .conversationId(conversationId)
                .title(title)
                .build();

        // 初始化（设置创建时间、更新时间、状态等）
        conversation.init();

        // 保存到数据库
        this.save(conversation);

        log.info("创建会话成功：conversationId={}, title={}", conversationId, title);
        return conversation;
    }

    @Override
    public ChatConversation getConversation(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "会话ID不能为空");
        }

        ChatConversation conversation = baseMapper.findByConversationId(conversationId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }

        return conversation;
    }

    @Override
    public List<ChatConversation> listConversations(int page, int size) {
        if (page < 1) {
            page = 1;
        }
        if (size < 1 || size > 100) {
            size = 20;  // 默认每页20条
        }

        // 计算偏移量
        int offset = (page - 1) * size;

        // 查询活跃会话（status=1）
        return baseMapper.findConversationsPage(offset, size);
    }

    @Override
    public void deleteConversation(String conversationId) {
        ChatConversation conversation = getConversation(conversationId);

        // 软删除：设置status=3
        conversation.setStatus(ChatConversation.ConversationStatus.DELETED.getCode());
        conversation.setUpdatedAt(LocalDateTime.now());

        this.updateById(conversation);

        log.info("删除会话成功：conversationId={}", conversationId);
    }

    @Override
    public void updateConversationTitle(String conversationId, String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "标题不能为空");
        }

        ChatConversation conversation = getConversation(conversationId);
        conversation.setTitle(title);
        conversation.setUpdatedAt(LocalDateTime.now());

        this.updateById(conversation);

        log.info("更新会话标题成功：conversationId={}, title={}", conversationId, title);
    }

    @Override
    public void archiveConversation(String conversationId) {
        ChatConversation conversation = getConversation(conversationId);

        // 归档：设置status=2
        conversation.setStatus(ChatConversation.ConversationStatus.ARCHIVED.getCode());
        conversation.setUpdatedAt(LocalDateTime.now());

        this.updateById(conversation);

        log.info("归档会话成功：conversationId={}", conversationId);
    }

    @Override
    public Integer countActiveConversations() {
        return baseMapper.countActiveConversations();
    }
}