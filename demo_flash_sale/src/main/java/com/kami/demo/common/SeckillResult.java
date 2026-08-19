package com.kami.demo.common;

import lombok.Data;

/**
 * @author kami
 * @description 抢购操作结果封装对象，包含是否成功及提示信息
 */
@Data
public class SeckillResult {
    private boolean success;
    private String message;

    public static SeckillResult success(String message) {
        SeckillResult result = new SeckillResult();
        result.setSuccess(true);
        result.setMessage(message);
        return result;
    }

    public static SeckillResult fail(String message) {
        SeckillResult result = new SeckillResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
}