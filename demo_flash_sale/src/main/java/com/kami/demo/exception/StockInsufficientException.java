package com.kami.demo.exception;

/**
 * @author kami
 * @description 库存不足业务异常，用于区分业务异常与瞬时异常，使消息消费者能够对库存不足消息做丢弃处理而不无限重试
 */
public class StockInsufficientException extends RuntimeException {

    public StockInsufficientException(String message) {
        super(message);
    }
}