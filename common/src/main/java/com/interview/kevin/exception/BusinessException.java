package com.interview.kevin.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;
    private final String msg;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.msg = errorCode.getMessage();
    }
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.msg = errorCode.getMessage();

    }
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.msg = message;
    }
    public BusinessException(String message) {
        super(message);
        this.code = ErrorCode.INTERNAL_ERROR.getCode();
        this.msg  = message;
    }
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = ErrorCode.INTERNAL_ERROR.getCode();
        this.msg  = message;
    }

}
