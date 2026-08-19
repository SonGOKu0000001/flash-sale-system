package com.kami.demo.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author kami
 * @description RabbitMQ 交换机、队列、死信队列及 JSON 消息转换器配置，为秒杀异步下单提供消息通道
 */
@Configuration
public class RabbitMQConfig {

    public static final String SECKILL_EXCHANGE = "seckill.exchange";
    public static final String SECKILL_QUEUE = "seckill.queue";
    public static final String SECKILL_ROUTING_KEY = "seckill.order";
    public static final String SECKILL_DLX_EXCHANGE = "seckill.dlx.exchange";
    public static final String SECKILL_DLX_QUEUE = "seckill.dlx.queue";

    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(SECKILL_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", SECKILL_DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", SECKILL_ROUTING_KEY);
        args.put("x-message-ttl", 30000);
        return new Queue(SECKILL_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue())
                .to(seckillExchange()).with(SECKILL_ROUTING_KEY);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(SECKILL_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue dlxQueue() {
        return new Queue(SECKILL_DLX_QUEUE, true);
    }

    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue())
                .to(dlxExchange()).with(SECKILL_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}