package com.interview.aichat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 发送消息请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    /**
     * 消息内容
     */
    private String content;

    /**
     * 关联文件ID列表（可选，用于限定RAG检索范围）
     */
    private List<Long> fileIds;
}