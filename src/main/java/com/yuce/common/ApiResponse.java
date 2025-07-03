package com.yuce.common;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiResponse<T> {
    private int code;
    private String message;
    private LocalDateTime timestamp;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("请求成功");
        response.setTimestamp(LocalDateTime.now());
        response.setData(data);
        return response;
    }

    /**
     * @desc 请求参数异常
     * @return
     * @param <T>
     */
    public static <T> ApiResponse<T> fail(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}