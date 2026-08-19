package com.kami.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author kami
 * @description 秒杀系统自定义配置属性，对应 application.yml 中 seckill 前缀下的限流、分布式锁、结果过期时间等配置项
 */
@Data
@Component
@ConfigurationProperties(prefix = "seckill")
public class SeckillProperties {

    private RateLimit rateLimit = new RateLimit();

    private Lock lock = new Lock();

    private Expire expire = new Expire();

    @Data
    public static class RateLimit {
        private int rate = 100;
        private int burst = 200;
    }

    @Data
    public static class Lock {
        private long waitTime = 3;
        private long leaseTime = 10;
    }

    @Data
    public static class Expire {
        private int resultTtl = 300;
    }
}