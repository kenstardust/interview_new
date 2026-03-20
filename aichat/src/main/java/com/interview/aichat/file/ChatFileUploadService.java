package com.interview.aichat.file;

import com.interview.aichat.stream.StreamTaskPublisher;
import com.interview.aichat.stream.TaskStatusService;
import com.interview.kevin.constant.TaskStatusCode;
import com.interview.kevin.exception.BusinessException;
import com.interview.kevin.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatFileUploadService {

    private final FileStorageService fileStorageService;
    private final StreamTaskPublisher streamTaskPublisher;
    private final TaskStatusService taskStatusService;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;//10MB

    public Map<String,Object> uploadFileAndAnalyze(MultipartFile file, String taskType) {
        validateFile(file);

        String storageKey = fileStorageService.uploadChatFile(file);
        String taskId = UUID.randomUUID().toString().replace("-", "");
        String streamRecordId = streamTaskPublisher.publish(taskId, taskType, storageKey, file.getOriginalFilename());

        taskStatusService.initPending(taskId, taskType, storageKey);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId);
        result.put("taskType", taskType);
        result.put("status", TaskStatusCode.PENDING.name());
        result.put("storageKey", storageKey);
        result.put("streamRecordId", streamRecordId);
        return result;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件大小超过10MB限制");
        }
    }

}
