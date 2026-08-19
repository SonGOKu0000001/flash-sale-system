package com.kami.demo.service;

import com.kami.demo.config.RabbitMQConfig;
import com.kami.demo.dto.SeckillMessage;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author kami
 * @description 秒杀消息生产者，抢购成功后把下单消息发送到 RabbitMQ 队列，交给消费者异步处理
 */
@Service
public class SeckillMessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendSeckillMessage(SeckillMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECKILL_EXCHANGE,
                RabbitMQConfig.SECKILL_ROUTING_KEY,
                message,
                msg -> {
                    msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return msg;
                }
        );
    }
}