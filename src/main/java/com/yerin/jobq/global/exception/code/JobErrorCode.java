package com.yerin.jobq.global.exception.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum JobErrorCode implements ErrorCode {
    JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "작업을 찾을 수 없습니다.", "JOB-001"),
    JOB_NOT_IN_DLQ(HttpStatus.BAD_REQUEST, "DLQ 상태의 작업만 재실행할 수 있습니다.", "JOB-002");

    private final HttpStatus httpStatus;
    private final String message;
    private final String code;
}
