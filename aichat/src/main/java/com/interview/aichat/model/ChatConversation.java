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
 * 会话模型：管理用户对话会话
 *
 * 对应表：chat_conversation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_conversation")
public class ChatConversation {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话UUID（对外引用ID）
     */
    private String conversationId;

    /**
     * 会话标题（自动生成或用户编辑）
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
     * 1=ACTIVE (活跃)
     * 2=ARCHIVED (归档)
     * 3=DELETED (已删除)
     */
    private Integer status;

    /**
     * 初始化会话
     * 创建时间、更新时间设置为当前时间
     * 状态默认为活跃（1）
     */
    public void init() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.messageCount = 0;
        this.status = 1;  // ACTIVE
    }

    /**
     * 会话状态枚举
     */
    public enum ConversationStatus {
        ACTIVE(1, "活跃"),
        ARCHIVED(2, "归档"),
        DELETED(3, "已删除");

        private final int code;
        private final String description;

        ConversationStatus(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }
}