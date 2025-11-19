package com.yerin.jobq.global.exception;

import com.yerin.jobq.global.dto.ErrorResponse;
import com.yerin.jobq.global.exception.code.CommonErrorCode;
import com.yerin.jobq.global.exception.code.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionController {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException e,
                                                            HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("AppException: {}, path={} {}", errorCode.getMessage(),
                request.getMethod(), request.getRequestURI());

        ErrorResponse body = ErrorResponse.of(errorCode, request);
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e,
                                                          HttpServletRequest request) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = (fieldError != null)
                ? fieldError.getDefaultMessage()
                : "입력값이 유효하지 않습니다.";

        ErrorCode errorCode = CommonErrorCode.INVALID_PARAMETER.withDetail(msg);
        log.error("Validation failed: {}, path={} {}", msg,
                request.getMethod(), request.getRequestURI());

        ErrorResponse body = ErrorResponse.of(errorCode, request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception e,
                                                   HttpServletRequest request) {
        log.error("Unhandled exception: ", e);
        ErrorResponse body = ErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR, request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
