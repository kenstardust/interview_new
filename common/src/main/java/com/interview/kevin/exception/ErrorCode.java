package com.interview.kevin.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(200,"成功"),
    BAD_REQUEST(400,"请求参数错误"),
    UNAUTHORIZED(401,"未授权"),
    FORBIDDEN(403,"禁止访问"),
    NOT_FOUND(404,"资源不存在"),
    INTERNAL_ERROR(500,"服务器内部错误"),


    //对话模块错误
    CHAT_SESSION_NOTFOUND(3001,"对话未找到"),
    CHAT_SESSION_EXPIRED(3002,"对话已过期"),
    CHAT_QUESTION_NOT_FOUND(3003,"对话不存在"),
    CHAT_ALREADY_COMPLETE(3004,"对话已完成"),
    CHAT_FAILED(3005,"对话失败"),
    CHAT_QUESTION_GENERATION_FAILED(3006,"对话问题生成失败"),
    CHAT_NOT_COMPLETE(3007,"对话未完成"),

    //存储模块错误
    STORAGE_UPLOAD_FAILED(4001, "文件上传失败"),
    STORAGE_DOWNLOAD_FAILED(4002, "文件下载失败"),
    STORAGE_DELETE_FAILED(4003, "文件删除失败"),

    //导出错误
    EXPORT_PDF_FAILED(5001, "PDF导出失败"),
    //知识库模块错误
    KNOWLEDGE_BASE_NOT_FOUND(6001, "知识库不存在"),
    KNOWLEDGE_BASE_PARSE_FAILED(6002, "知识库文件解析失败"),
    KNOWLEDGE_BASE_UPLOAD_FAILED(6003, "知识库上传失败"),
    KNOWLEDGE_BASE_QUERY_FAILED(6004, "知识库查询失败"),
    KNOWLEDGE_BASE_DELETE_FAILED(6005, "知识库删除失败"),
    KNOWLEDGE_BASE_VECTORIZATION_FAILED(6006, "知识库向量化失败"),

    //AI服务错误
    AI_SERVICE_UNAVAILABLE(7001, "AI服务暂时不可用，请稍后重试"),
    AI_SERVICE_TIMEOUT(7002, "AI服务响应超时"),
    AI_SERVICE_ERROR(7003, "AI服务调用失败"),
    AI_API_KEY_INVALID(7004, "AI服务密钥无效"),
    AI_RATE_LIMIT_EXCEEDED(7005, "AI服务调用频率超限");

    private final Integer code;
    private final String message;

}
