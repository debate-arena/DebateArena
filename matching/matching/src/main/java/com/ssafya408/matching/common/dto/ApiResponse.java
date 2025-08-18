package com.ssafya408.matching.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private String status;
    private T data;

    // 성공 응답 생성 메서드
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status("success")
                .data(data)
                .build();
    }

    // 에러 응답 생성 메서드
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .status("error")
                .data((T) message)
                .build();
    }

    // 실패 응답 생성 메서드
    public static <T> ApiResponse<T> fail(T data) {
        return ApiResponse.<T>builder()
            .status("fail")
            .data((T) data)
            .build();
    }

    // 정보 응답 생성 메서드
    public static <T> ApiResponse<T> info(T data) {
        return ApiResponse.<T>builder()
                .status("info")
                .data(data)
                .build();
    }

    // 경고 응답 생성 메서드
    public static <T> ApiResponse<T> warning(T data) {
        return ApiResponse.<T>builder()
                .status("warning")
                .data(data)
                .build();
    }
}