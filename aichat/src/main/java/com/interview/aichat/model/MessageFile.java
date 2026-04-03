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
 * 消息-文件关联模型：记录消息引用的文件
 *
 * 对应表：message_file
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("message_file")
public class MessageFile {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息ID（外键关联）
     */
    private Long messageId;

    /**
     * 文件ID（外键关联）
     */
    private Long fileId;

    /**
     * 附加时间
     */
    private LocalDateTime attachedAt;

    /**
     * 文件用途
     * "knowledge" - 知识库文件（用于RAG检索）
     * "reference" - 参考资料
     * "attachment" - 普通附件
     */
    private String purpose;

    /**
     * 初始化关联
     * 附加时间设置为当前时间
     * 用途默认为knowledge
     */
    public void init() {
        this.attachedAt = LocalDateTime.now();
        this.purpose = "knowledge";  // 默认为知识库用途
    }

    /**
     * 文件用途枚举
     */
    public enum FilePurpose {
        KNOWLEDGE("knowledge", "知识库"),
        REFERENCE("reference", "参考资料"),
        ATTACHMENT("attachment", "附件");

        private final String purpose;
        private final String description;

        FilePurpose(String purpose, String description) {
            this.purpose = purpose;
            this.description = description;
        }

        public String getPurpose() {
            return purpose;
        }

        public String getDescription() {
            return description;
        }
    }
}