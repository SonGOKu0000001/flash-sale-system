package com.kami.demo.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.List;

/**
 * @author kami
 * @description Redis 预减库存服务，活动开始前将库存加载到 Redis，抢购时通过 Lua 脚本原子扣减库存，防止超卖
 */
@Service
public class SeckillStockService {

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";

    private static final String STOCK_DEDUCT_SCRIPT =
            "local stock = tonumber(redis.call('GET', KEYS[1]))\n" +
                    "if stock == nil then return -1 end\n" +
                    "if stock < tonumber(ARGV[1]) then return 0 end\n" +
                    "redis.call('DECRBY', KEYS[1], tonumber(ARGV[1]))\n" +
                    "return 1";

    @Autowired
    private StringRedisTemplate redisTemplate;

    private DefaultRedisScript<Long> deductScript;

    @PostConstruct
    public void init() {
        deductScript = new DefaultRedisScript<>();
        deductScript.setScriptText(STOCK_DEDUCT_SCRIPT);
        deductScript.setResultType(Long.class);
    }

    /**
     * 初始化活动库存到 Redis，并设置过期时间（活动结束时间 + 1 天）
     */
    public void initStock(Long activityId, Integer stock, Date endTime) {
        String key = STOCK_KEY_PREFIX + activityId;
        long ttlSeconds = endTime == null
                ? 86_400L
                : (endTime.getTime() + 86_400_000L - System.currentTimeMillis()) / 1000;
        redisTemplate.opsForValue().set(key, stock.toString(), Duration.ofSeconds(Math.max(ttlSeconds, 60)));
    }

    /**
     * 扣减库存
     * @return -1: 库存未初始化  0: 库存不足  1: 扣减成功
     */
    public Long deductStock(Long activityId, Integer quantity) {
        String key = STOCK_KEY_PREFIX + activityId;
        return redisTemplate.execute(deductScript, List.of(key), quantity.toString());
    }

    /**
     * 获取当前库存
     */
    public Integer getStock(Long activityId) {
        String key = STOCK_KEY_PREFIX + activityId;
        String stock = redisTemplate.opsForValue().get(key);
        return stock == null ? 0 : Integer.parseInt(stock);
    }
}