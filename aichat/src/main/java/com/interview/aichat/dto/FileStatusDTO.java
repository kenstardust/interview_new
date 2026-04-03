package com.interview.aichat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件状态响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileStatusDTO {

    /**
     * 文件ID
     */
    private Long fileId;

    /**
     * 任务状态（PENDING, PROCESSING, COMPLETED, FAILED）
     */
    private String taskstatus;

    /**
     * 分析错误信息（失败时）
     */
    private String analyzeError;

    /**
     * 分块数量（处理完成后）
     */
    private Integer chunkCount;
}