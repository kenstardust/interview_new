package com.interview.aichat.dto;

import com.interview.aichat.model.ChatConversation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话DTO：会话列表项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {

    /**
     * 会话UUID
     */
    private String conversationId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 消息数量
     */
    private Integer messageCount;

    /**
     * 会话状态（1=ACTIVE, 2=ARCHIVED, 3=DELETED）
     */
    private Integer status;

    /**
     * 从Model转换为DTO
     */
    public static ConversationDTO fromModel(ChatConversation conversation) {
        return ConversationDTO.builder()
                .conversationId(conversation.getConversationId())
                .title(conversation.getTitle())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .messageCount(conversation.getMessageCount())
                .status(conversation.getStatus())
                .build();
    }
}