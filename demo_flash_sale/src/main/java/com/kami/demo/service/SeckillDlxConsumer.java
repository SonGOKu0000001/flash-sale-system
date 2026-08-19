package com.kami.demo.service;

import com.kami.demo.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author kami
 * @description 死信队列消费者，处理在业务队列中超时未消费而被投递到死信队列的秒杀消息，记录告警日志并手动确认
 */
@Slf4j
@Component
@RabbitListener(queues = RabbitMQConfig.SECKILL_DLX_QUEUE)
public class SeckillDlxConsumer {

    @RabbitHandler
    public void handleDlxMessage(Message message,
                                 Channel channel,
                                 @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        log.warn("收到超时未处理的死信消息：{}", new String(message.getBody(), StandardCharsets.UTF_8));
        channel.basicAck(tag, false);
    }
}