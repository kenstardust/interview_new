package com.industry.aichat.file;

import com.industry.aichat.model.ChatFile;
import com.industry.aichat.service.ChatFileService;
import com.industry.aichat.service.rag.RAGOrchestrationService;
import com.industry.kevin.exception.BusinessException;
import com.industry.kevin.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传+分析门面服务
 *
 * 处理链路：上传S3 → Tika解析 → 文本清洗 → 保存数据库 → 异步RAG处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatFileUploadService {

    private final FileStorageService fileStorageService;
    private final DocumentParseService documentParseService;
    private final ChatFileService chatFileService;
    private final RAGOrchestrationService ragOrchestrationService;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * 上传文件并分析
     *
     * @param file 上传的文件
     * @return 包含fileId的Map
     */
    public Map<String, Object> uploadFileAndAnalyze(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        // 1. 校验文件
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件大小不能超过10MB");
        }

        log.info("开始处理文件：filename={}, size={}", originalFilename, file.getSize());

        // 2. 上传到S3
        String storageKey;
        try {
            storageKey = fileStorageService.uploadChatFile(file);
            log.info("文件已上传到S3：storageKey={}", storageKey);
        } catch (Exception e) {
            log.error("文件上传S3失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件上传失败：" + e.getMessage());
        }

        // 3. 保存文件记录到数据库（状态PENDING）
        ChatFile chatFile = ChatFile.builder()
                .originalfilename(originalFilename)
                .filesize(file.getSize())
                .contenttype(file.getContentType())
                .storagekey(storageKey)
                .taskstatus("PENDING")
                .build();
        chatFileService.save(chatFile);
        log.info("文件记录已保存：fileId={}", chatFile.getId());

        // 4. 异步触发：解析 + RAG处理
        processAsync(chatFile.getId(), file);

        // 5. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("fileId", chatFile.getId());
        result.put("storageKey", storageKey);
        result.put("taskstatus", "PENDING");
        result.put("originalFilename", originalFilename);
        return result;
    }

    /**
     * 异步处理文件：解析 → 清洗 → 保存文本 → RAG分块向量化
     */
    @Async("ragTaskExecutor")
    public void processAsync(Long fileId, MultipartFile file) {
        ChatFile chatFile = chatFileService.getById(fileId);
        if (chatFile == null) {
            log.error("文件不存在：fileId={}", fileId);
            return;
        }

        try {
            // 更新状态为处理中
            chatFile.setTaskstatus("PROCESSING");
            chatFileService.updateById(chatFile);

            // 下载文件字节并解析
            byte[] fileBytes = fileStorageService.downloadFile(chatFile.getStoragekey());
            String parsedText = documentParseService.parseContent(fileBytes, chatFile.getOriginalfilename());

            // 保存解析后的文本
            chatFile.setFiletext(parsedText);

            if (parsedText == null || parsedText.trim().isEmpty()) {
                chatFile.setTaskstatus("FAILED");
                chatFile.setAnalyzeError("文件解析结果为空");
                chatFileService.updateById(chatFile);
                log.warn("文件解析结果为空：fileId={}", fileId);
                return;
            }

            // 更新状态为已完成
            chatFile.setTaskstatus("COMPLETED");
            chatFileService.updateById(chatFile);
            log.info("文件解析完成：fileId={}, textLength={}", fileId, parsedText.length());

            // 触发RAG异步处理（分块 + 向量化）
            ragOrchestrationService.processAndIndexDocumentAsync(chatFile);

        } catch (Exception e) {
            log.error("文件处理失败：fileId={}, error={}", fileId, e.getMessage(), e);
            chatFile.setTaskstatus("FAILED");
            chatFile.setAnalyzeError(e.getMessage());
            chatFileService.updateById(chatFile);
        }
    }
}
