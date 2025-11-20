package com.yerin.jobq.global.exception.code;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    HttpStatus getHttpStatus();
    String getMessage();
    String getCode();

    default ErrorCode withDetail(String detailMessage) {
        return new ErrorCode() {
            @Override
            public HttpStatus getHttpStatus() {
                return ErrorCode.this.getHttpStatus();
            }

            @Override
            public String getMessage() {
                return detailMessage;
            }

            @Override
            public String getCode() {
                return ErrorCode.this.getCode();
            }
        };
    }
}
