package com.interview.aichat.dto;

import com.interview.aichat.model.ChatConversation;
import com.interview.aichat.model.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话详情DTO：包含会话信息和消息列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDetailDTO {

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
     * 会话状态
     */
    private Integer status;

    /**
     * 消息列表
     */
    private List<MessageDTO> messages;

    /**
     * 从Model转换为DTO
     */
    public static ConversationDetailDTO fromModel(ChatConversation conversation, List<ChatMessage> messages) {
        return ConversationDetailDTO.builder()
                .conversationId(conversation.getConversationId())
                .title(conversation.getTitle())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .messageCount(conversation.getMessageCount())
                .status(conversation.getStatus())
                .messages(messages.stream()
                        .map(MessageDTO::fromModel)
                        .collect(Collectors.toList()))
                .build();
    }
}