package com.interview.aichat.controller;

import com.interview.aichat.dto.*;
import com.interview.aichat.model.ChatConversation;
import com.interview.aichat.model.ChatFile;
import com.interview.aichat.model.ChatMessage;
import com.interview.aichat.service.ChatConversationService;
import com.interview.aichat.service.ChatFileService;
import com.interview.aichat.service.ChatMessageService;
import com.interview.kevin.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天Controller：提供会话管理、消息发送、文件上传等API
 *
 * API端点：
 * - POST   /ai/v1/conversations                 - 创建会话
 * - GET    /ai/v1/conversations                 - 会话列表
 * - GET    /ai/v1/conversations/{id}            - 会话详情
 * - DELETE /ai/v1/conversations/{id}            - 删除会话
 * - POST   /ai/v1/conversations/{id}/messages   - 发送消息
 */
@Slf4j
@RestController
@RequestMapping("/ai/v1")
public class ChatController {

    @Resource
    private ChatConversationService conversationService;

    @Resource
    private ChatMessageService messageService;

    @Resource
    private ChatFileService chatFileService;

    // ==================== 会话管理 API ====================

    /**
     * 创建新会话
     *
     * @param request 创建会话请求
     * @return 会话DTO
     */
    @PostMapping("/conversations")
    public Result<ConversationDTO> createConversation(@RequestBody CreateConversationRequest request) {
        log.info("创建会话：title={}", request.getTitle());

        ChatConversation conversation = conversationService.createConversation(request.getTitle());

        return Result.success(ConversationDTO.fromModel(conversation));
    }

    /**
     * 查询会话列表（分页）
     *
     * @param page 页码（默认1）
     * @param size 每页数量（默认20）
     * @return 会话列表
     */
    @GetMapping("/conversations")
    public Result<List<ConversationDTO>> listConversations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("查询会话列表：page={}, size={}", page, size);

        List<ChatConversation> conversations = conversationService.listConversations(page, size);

        List<ConversationDTO> dtoList = conversations.stream()
                .map(ConversationDTO::fromModel)
                .collect(Collectors.toList());

        return Result.success(dtoList);
    }

    /**
     * 查询会话详情（包含消息列表）
     *
     * @param conversationId 会话UUID
     * @return 会话详情DTO
     */
    @GetMapping("/conversations/{conversationId}")
    public Result<ConversationDetailDTO> getConversation(@PathVariable String conversationId) {
        log.info("查询会话详情：conversationId={}", conversationId);

        // 查询会话
        ChatConversation conversation = conversationService.getConversation(conversationId);

        // 查询消息列表
        List<ChatMessage> messages = messageService.getConversationMessages(conversationId);

        // 转换为DTO
        ConversationDetailDTO detailDTO = ConversationDetailDTO.fromModel(conversation, messages);

        return Result.success(detailDTO);
    }

    /**
     * 删除会话
     *
     * @param conversationId 会话UUID
     * @return 成功响应
     */
    @DeleteMapping("/conversations/{conversationId}")
    public Result<Void> deleteConversation(@PathVariable String conversationId) {
        log.info("删除会话：conversationId={}", conversationId);

        conversationService.deleteConversation(conversationId);

        return Result.success();
    }

    /**
     * 更新会话标题
     *
     * @param conversationId 会话UUID
     * @param request        更新请求
     * @return 成功响应
     */
    @PutMapping("/conversations/{conversationId}/title")
    public Result<Void> updateConversationTitle(
            @PathVariable String conversationId,
            @RequestBody CreateConversationRequest request) {

        log.info("更新会话标题：conversationId={}, title={}", conversationId, request.getTitle());

        conversationService.updateConversationTitle(conversationId, request.getTitle());

        return Result.success();
    }

    // ==================== 消息发送 API ====================

    /**
     * 发送消息（同步，简单版本，无RAG）
     *
     * 注意：此端点为简单实现，不包含RAG检索增强
     * 后续会添加 /conversations/{id}/messages/stream 端点支持RAG和流式响应
     *
     * @param conversationId 会话UUID
     * @param request        发送消息请求
     * @return 消息DTO
     */
    @PostMapping("/conversations/{conversationId}/messages")
    public Result<MessageDTO> sendMessage(
            @PathVariable String conversationId,
            @RequestBody SendMessageRequest request) {

        log.info("发送消息：conversationId={}, content={}", conversationId, request.getContent());

        // 保存用户消息
        ChatMessage userMessage = messageService.addMessage(
                conversationId,
                "user",
                request.getContent()
        );

        // TODO: 实现RAG检索增强和LLM调用
        // 目前先返回简单的固定响应
        String assistantContent = "收到您的消息：" + request.getContent() + "\n\n（系统正在建设中，暂不支持智能回复）";

        // 保存助手消息
        ChatMessage assistantMessage = messageService.addMessage(
                conversationId,
                "assistant",
                assistantContent
        );

        return Result.success(MessageDTO.fromModel(assistantMessage));
    }

    // ==================== 文件上传 API ====================

    /**
     * 上传文件
     *
     * 注意：此端点为占位实现，完整实现需要集成文件处理和RAG服务
     *
     * @param file 文件（MultipartFile）
     * @return 文件上传DTO
     */
    @PostMapping("/files/upload")
    public Result<FileUploadDTO> uploadFile(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        log.info("上传文件：filename={}, size={}", file.getOriginalFilename(), file.getSize());

        // TODO: 实现文件上传和异步处理
        // 1. 上传到S3
        // 2. 解析文档
        // 3. 触发RAG处理（分块、向量化）

        // 目前返回占位响应
        FileUploadDTO uploadDTO = FileUploadDTO.builder()
                .fileId(1L)  // 占位ID
                .storageKey("placeholder")
                .taskstatus("PENDING")
                .originalFilename(file.getOriginalFilename())
                .build();

        return Result.success(uploadDTO);
    }

    /**
     * 查询文件处理状态
     *
     * @param fileId 文件ID
     * @return 文件状态DTO
     */
    @GetMapping("/files/{fileId}/status")
    public Result<FileStatusDTO> getFileStatus(@PathVariable Long fileId) {
        log.info("查询文件状态：fileId={}", fileId);

        // TODO: 实现文件状态查询
        // 查询ChatFile表的taskstatus字段

        // 目前返回占位响应
        FileStatusDTO statusDTO = FileStatusDTO.builder()
                .fileId(fileId)
                .taskstatus("PENDING")
                .chunkCount(0)
                .build();

        return Result.success(statusDTO);
    }

    // ==================== 测试端点 ====================

    /**
     * 测试端点
     *
     * @return 测试字符串
     */
    @RequestMapping("/test")
    public String test() {
        return "Hello World - RAG Chat System v1.0";
    }
}