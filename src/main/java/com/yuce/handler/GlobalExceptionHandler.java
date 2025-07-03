package com.yuce.handler;

import com.yuce.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import javax.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理 @RequestBody 参数校验失败
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldError().getDefaultMessage();
        return ApiResponse.fail(HttpStatus.BAD_REQUEST.value(), errorMessage);
    }

    // 处理 @ModelAttribute 参数校验失败
    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindException(BindException ex) {
        String errorMessage = ex.getBindingResult().getFieldError().getDefaultMessage();
        return ApiResponse.fail(HttpStatus.BAD_REQUEST.value(), errorMessage);
    }

    // 处理 @RequestParam 校验失败
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException ex) {
        String errorMessage = ex.getConstraintViolations().iterator().next().getMessage();
        return ApiResponse.fail(HttpStatus.BAD_REQUEST.value(), errorMessage);
    }

    // 其他异常兜底
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        return ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统错误：" + ex.getMessage());
    }
}