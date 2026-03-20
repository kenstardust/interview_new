package com.interview.aichat.controller;

import com.interview.aichat.file.ChatFileUploadService;
import com.interview.aichat.stream.TaskStatusService;
import com.interview.kevin.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ai/v1")
@RequiredArgsConstructor
public class ChatController {
    private final ChatFileUploadService chatFileUploadService;
    private final TaskStatusService taskStatusService;

    @GetMapping("/test")
    public String chatController(){
        return "Hello World";
    }

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                              @RequestParam(value = "taskType", defaultValue = "knowledgebase:vectorize")
                                              String taskType) {
        return Result.success(chatFileUploadService.uploadFileAndAnalyze(file, taskType));
    }

    @GetMapping("/task/status")
    public Result<Map<Object, Object>> taskStatus(@RequestParam("taskId") String taskId) {
        return Result.success(taskStatusService.getStatus(taskId));
    }


}
