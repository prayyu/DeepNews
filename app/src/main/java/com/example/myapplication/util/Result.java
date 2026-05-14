package com.example.myapplication.util;

/**
 * 统一结果包装，用于 Repository 层返回操作结果。
 * 成功时携带数据 T，失败时携带错误信息。
 */
public class Result<T> {
    private final T data;
    private final String error;
    private final boolean isSuccess;

    private Result(T data, String error, boolean isSuccess) {
        this.data = data;
        this.error = error;
        this.isSuccess = isSuccess;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(data, null, true);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(null, message, false);
    }

    public boolean isSuccess() { return isSuccess; }
    public T getData() { return data; }
    public String getError() { return error; }
}
