package com.industry.aichat.controller;

import com.industry.aichat.dto.*;
import com.industry.aichat.model.ChatConversation;
import com.industry.aichat.model.ChatFile;
import com.industry.aichat.model.ChatMessage;
import com.industry.aichat.model.DocumentChunk;
import com.industry.aichat.service.ChatConversationService;
import com.industry.aichat.service.ChatFileService;
import com.industry.aichat.service.ChatMessageService;
import com.industry.aichat.service.rag.RetrievalService;
import com.industry.aichat.file.ChatFileUploadService;
import com.industry.kevin.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.Resource;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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

    @Resource
    private RetrievalService retrievalService;

    @Resource
    private ChatModel chatModel;

    @Resource
    private ChatFileUploadService chatFileUploadService;

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

    /**
     * 发送消息并流式返回AI回答（SSE）
     *
     * @param conversationId 会话ID
     * @param request        发送消息请求
     * @return SSE流
     */
    @PostMapping("/conversations/{conversationId}/messages/stream")
    public SseEmitter streamMessage(
            @PathVariable String conversationId,
            @RequestBody SendMessageRequest request) {

        log.info("流式发送消息：conversationId={}, content={}, fileIds={}",
                conversationId, request.getContent(), request.getFileIds());

        // 创建SSE发射器，60秒超时
        SseEmitter emitter = new SseEmitter(60000L);

        // 异步处理，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 保存用户消息
                ChatMessage userMessage = messageService.addMessage(
                        conversationId,
                        "user",
                        request.getContent()
                );

                // 2. RAG检索上下文（如果提供了fileIds）
                String context = "";
                if (request.getFileIds() != null && !request.getFileIds().isEmpty()) {
                    log.info("RAG检索开始：fileIds={}", request.getFileIds());
                    List<DocumentChunk> chunks = retrievalService.retrieveRelevantChunksInFiles(
                            request.getContent(),
                            request.getFileIds(),
                            5  // Top-K
                    );
                    context = retrievalService.buildContextString(chunks);
                    log.info("RAG检索完成：找到{}个相关文档块", chunks.size());
                } else {
                    // 如果没有指定文件，使用全局检索
                    log.info("未指定文件，使用全局RAG检索");
                    List<DocumentChunk> chunks = retrievalService.retrieveRelevantChunks(
                            request.getContent(),
                            5  // Top-K
                    );
                    context = retrievalService.buildContextString(chunks);
                    log.info("全局RAG检索完成：找到{}个相关文档块", chunks.size());
                }

                // 3. 构建Prompt（包含系统提示和上下文）
                StringBuilder promptBuilder = new StringBuilder();

                // 系统提示
                promptBuilder.append("你是工业软件智能问答助手，专门回答工业软件相关的问题。\n\n");

                // 如果有RAG上下文，添加到Prompt
                if (!context.isEmpty()) {
                    promptBuilder.append("参考以下知识库内容回答问题：\n\n");
                    promptBuilder.append(context);
                    promptBuilder.append("\n\n");
                }

                // 用户问题
                promptBuilder.append("用户问题：").append(request.getContent());

                String finalPrompt = promptBuilder.toString();
                log.debug("最终Prompt长度：{}字符", finalPrompt.length());

                // 4. 调用LLM流式生成
                StringBuilder fullResponse = new StringBuilder();

                try {
                    // 使用ChatModel.stream()进行流式调用
                    Prompt prompt = new Prompt(finalPrompt);
                    Flux<ChatResponse> responseFlux = chatModel.stream(prompt);

                    // 订阅流式响应
                    responseFlux.subscribe(
                            chatResponse -> {
                                try {
                                    // 提取内容
                                    if (chatResponse.getResult() != null &&
                                        chatResponse.getResult().getOutput() != null) {

                                        String content = chatResponse.getResult().getOutput().getText();
                                        if (content != null && !content.isEmpty()) {
                                            fullResponse.append(content);

                                            // 通过SSE发送给前端
                                            emitter.send(SseEmitter.event()
                                                    .name("message")
                                                    .data(content));
                                        }
                                    }
                                } catch (Exception e) {
                                    log.error("发送SSE消息失败", e);
                                }
                            },
                            error -> {
                                // 错误处理
                                log.error("LLM流式调用失败", error);
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("error")
                                            .data("LLM调用失败：" + error.getMessage()));
                                    emitter.completeWithError(error);
                                } catch (IOException ex) {
                                    log.error("发送错误事件失败", ex);
                                }
                            },
                            () -> {
                                // 完成处理
                                try {
                                    // 5. 保存完整的AI回答
                                    String responseText = fullResponse.toString();
                                    if (!responseText.isEmpty()) {
                                        ChatMessage assistantMessage = messageService.addMessage(
                                                conversationId,
                                                "assistant",
                                                responseText
                                        );
                                        log.info("AI回答已保存：messageId={}", assistantMessage.getId());
                                    }

                                    // 6. 发送完成事件
                                    emitter.send(SseEmitter.event().name("complete").data(""));
                                    emitter.complete();

                                    log.info("流式消息发送完成：conversationId={}", conversationId);
                                } catch (Exception e) {
                                    log.error("完成处理失败", e);
                                }
                            }
                    );

                } catch (Exception e) {
                    log.error("LLM调用异常", e);
                    // 降级方案：返回错误提示
                    String errorMessage = "抱歉，AI服务暂时不可用。错误信息：" + e.getMessage();
                    emitter.send(SseEmitter.event().name("message").data(errorMessage));
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                    emitter.completeWithError(e);
                }

            } catch (Exception e) {
                log.error("流式消息处理失败", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(e.getMessage()));
                    emitter.completeWithError(e);
                } catch (IOException ex) {
                    log.error("发送错误事件失败", ex);
                }
            }
        });

        return emitter;
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

        try {
            Map<String, Object> result = chatFileUploadService.uploadFileAndAnalyze(file);

            FileUploadDTO uploadDTO = FileUploadDTO.builder()
                    .fileId((Long) result.get("fileId"))
                    .storageKey((String) result.get("storageKey"))
                    .taskstatus((String) result.get("taskstatus"))
                    .originalFilename((String) result.get("originalFilename"))
                    .build();

            return Result.success(uploadDTO);
        } catch (Exception e) {
            log.error("文件上传失败：filename={}, error={}", file.getOriginalFilename(), e.getMessage(), e);
            return Result.error("文件上传失败：" + e.getMessage());
        }
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