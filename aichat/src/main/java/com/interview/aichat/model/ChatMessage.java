package com.interview.aichat.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息模型：存储对话中的所有消息
 *
 * 对应表：chat_message
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_message")
public class ChatMessage {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话UUID（外键关联）
     */
    private String conversationId;

    /**
     * 消息角色
     * "user" - 用户消息
     * "assistant" - AI助手消息
     * "system" - 系统消息
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
     * Token计数（用于使用统计）
     */
    private Integer tokenCount;

    /**
     * JSON元数据
     * 包含：model（模型名称）、finish_reason、关联文件等
     */
    private String metadata;

    /**
     * 父消息ID（用于消息线程，可选）
     */
    private Long parentMessageId;

    /**
     * 初始化消息
     * 创建时间设置为当前时间
     */
    public void init() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 消息角色枚举
     */
    public enum MessageRole {
        USER("user", "用户"),
        ASSISTANT("assistant", "AI助手"),
        SYSTEM("system", "系统");

        private final String role;
        private final String description;

        MessageRole(String role, String description) {
            this.role = role;
            this.description = description;
        }

        public String getRole() {
            return role;
        }

        public String getDescription() {
            return description;
        }
    }
}