package com.interview.aichat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interview.aichat.mapper.ChatMessageMapper;
import com.interview.aichat.model.ChatMessage;
import com.interview.aichat.service.ChatMessageService;
import com.interview.kevin.exception.BusinessException;
import com.interview.kevin.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息Service实现类
 */
@Slf4j
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
        implements ChatMessageService {

    @Override
    public ChatMessage addMessage(String conversationId, String role, String content) {
        return addMessage(conversationId, role, content, null);
    }

    @Override
    public ChatMessage addMessage(String conversationId, String role, String content, String metadata) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "会话ID不能为空");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息角色不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息内容不能为空");
        }

        // 创建消息对象
        ChatMessage message = ChatMessage.builder()
                .conversationId(conversationId)
                .role(role)
                .content(content)
                .metadata(metadata)
                .build();

        // 初始化（设置创建时间）
        message.init();

        // 估算token数量（简化估算：总字符数 / 4）
        int tokenCount = estimateTokenCount(content);
        message.setTokenCount(tokenCount);

        // 保存到数据库
        this.save(message);

        log.info("添加消息成功：conversationId={}, role={}, messageId={}", conversationId, role, message.getId());
        return message;
    }

    @Override
    public List<ChatMessage> getConversationMessages(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "会话ID不能为空");
        }

        return baseMapper.findByConversationId(conversationId);
    }

    @Override
    public List<ChatMessage> getRecentMessages(String conversationId, int limit) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "会话ID不能为空");
        }
        if (limit < 1) {
            limit = 10;  // 默认返回10条
        }

        return baseMapper.findRecentMessages(conversationId, limit);
    }

    @Override
    public void deleteMessage(Long messageId) {
        if (messageId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息ID不能为空");
        }

        ChatMessage message = this.getById(messageId);
        if (message == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "消息不存在");
        }

        this.removeById(messageId);

        log.info("删除消息成功：messageId={}", messageId);
    }

    @Override
    public Integer countMessages(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "会话ID不能为空");
        }

        return baseMapper.countByConversationId(conversationId);
    }

    /**
     * 估算token数量
     * 简单估算：中文约4字符=1token，英文约1字符=0.25token
     *
     * @param content 文本内容
     * @return 估算的token数量
     */
    private int estimateTokenCount(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        // 简化估算：总字符数 / 4（适用于中英文混合）
        return content.length() / 4;
    }
}