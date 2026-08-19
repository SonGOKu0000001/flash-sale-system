package com.kami.demo.common;

import lombok.Data;

/**
 * @author kami
 * @description 统一响应结果封装对象，提供成功与失败两种静态构造方法，供所有接口返回
 * @createDate 2026-08-07 16:16
 */
@Data
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> fail(String msg) {
        Result<T> result = new Result<>();
        result.setCode(400);
        result.setMsg(msg);
        return result;
    }
}