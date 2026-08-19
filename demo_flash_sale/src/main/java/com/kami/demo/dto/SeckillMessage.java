package com.kami.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author kami
 * @description 秒杀下单消息体，抢购成功后由生产者发送到 RabbitMQ，消费者据此异步创建订单
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeckillMessage implements Serializable {
    private Long userId;
    private Long activityId;
    private Long goodsId;
    private BigDecimal seckillPrice;
    private Long timestamp;
}