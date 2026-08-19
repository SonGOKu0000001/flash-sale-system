package com.kami.demo.service;

import com.kami.demo.config.RabbitMQConfig;
import com.kami.demo.dto.SeckillMessage;
import com.kami.demo.exception.StockInsufficientException;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author kami
 * @description 秒杀消息消费者，监听抢购消息队列，消费消息后异步创建订单并更新抢购结果缓存
 */
@Slf4j
@Component
@RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE)
public class SeckillMessageConsumer {

    @Autowired
    private OrderService orderService;

    @RabbitHandler
    public void handleSeckillMessage(SeckillMessage message,
                                     Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            orderService.createOrder(message);
            channel.basicAck(tag, false);
        } catch (StockInsufficientException e) {
            // 业务异常：库存不足，丢弃消息避免无限重试
            log.warn("库存不足，丢弃消息：{}", e.getMessage());
            try {
                channel.basicNack(tag, false, false);
            } catch (IOException ex) {
                log.error("消息确认失败", ex);
            }
        } catch (Exception e) {
            // 瞬时异常：Redis/MQ 等抖动，稍后重试
            log.error("秒杀消息消费异常，稍后重试", e);
            try {
                channel.basicNack(tag, false, true);
            } catch (IOException ex) {
                log.error("消息确认失败", ex);
            }
        }
    }
}