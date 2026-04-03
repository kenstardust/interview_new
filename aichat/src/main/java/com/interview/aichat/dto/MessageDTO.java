package com.interview.aichat.dto;

import com.interview.aichat.model.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息DTO：消息响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 消息角色（user/assistant/system）
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * Token计数
     */
    private Integer tokenCount;

    /**
     * 从Model转换为DTO
     */
    public static MessageDTO fromModel(ChatMessage message) {
        return MessageDTO.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .tokenCount(message.getTokenCount())
                .build();
    }
}