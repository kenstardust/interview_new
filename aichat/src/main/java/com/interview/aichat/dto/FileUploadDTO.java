package com.interview.aichat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadDTO {

    /**
     * 文件ID
     */
    private Long fileId;

    /**
     * 存储键（S3路径）
     */
    private String storageKey;

    /**
     * 任务状态（PENDING, PROCESSING, COMPLETED, FAILED）
     */
    private String taskstatus;

    /**
     * 原始文件名
     */
    private String originalFilename;
}