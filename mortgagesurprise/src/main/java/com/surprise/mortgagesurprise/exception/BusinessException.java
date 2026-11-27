package com.surprise.mortgagesurprise.exception;

import com.surprise.mortgagesurprise.dto.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode code;

    public BusinessException(ErrorCode code) {
        super(code.message != null ? code.message : code.name());
        this.code = code;
    }

    public BusinessException(ErrorCode code, String message) {
        super(message != null ? message : (code.message != null ? code.message : code.name()));
        this.code = code;
    }

    public BusinessException(String message) {
        this(ErrorCode.INTERNAL_ERROR, message);
    }
}
