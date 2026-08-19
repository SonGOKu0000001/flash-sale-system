package com.kami.demo.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author kami
 * @description 基于 Redis + Lua 令牌桶算法的接口限流服务，限制单个 IP 的请求频率，超出阈值的请求被拒绝
 */
@Service
public class RateLimiterService {

    private static final String IP_LIMIT_KEY_PREFIX = "seckill:limit:ip:";

    private static final String RATE_LIMIT_SCRIPT =
            "local key = KEYS[1]\n" +
                    "local now = tonumber(ARGV[1])\n" +
                    "local capacity = tonumber(ARGV[2])\n" +
                    "local rate = tonumber(ARGV[3])\n" +
                    "local data = redis.call('HMGET', key, 'tokens', 'last_time')\n" +
                    "local tokens = tonumber(data[1])\n" +
                    "local last_time = tonumber(data[2])\n" +
                    "if tokens == nil then\n" +
                    "    tokens = capacity\n" +
                    "    last_time = now\n" +
                    "end\n" +
                    "local time_passed = (now - last_time) / 1000\n" +
                    "local new_tokens = time_passed * rate\n" +
                    "tokens = math.min(tokens + new_tokens, capacity)\n" +
                    "if tokens >= 1 then\n" +
                    "    tokens = tokens - 1\n" +
                    "    redis.call('HMSET', key, 'tokens', tokens, 'last_time', now)\n" +
                    "    redis.call('EXPIRE', key, 60)\n" +
                    "    return 1\n" +
                    "else\n" +
                    "    redis.call('HMSET', key, 'tokens', tokens, 'last_time', now)\n" +
                    "    redis.call('EXPIRE', key, 60)\n" +
                    "    return 0\n" +
                    "end";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${seckill.rate-limit.rate:100}")
    private int rate;

    @Value("${seckill.rate-limit.burst:20}")
    private int burst;

    private DefaultRedisScript<Long> limitScript;

    @PostConstruct
    public void init() {
        limitScript = new DefaultRedisScript<>();
        limitScript.setScriptText(RATE_LIMIT_SCRIPT);
        limitScript.setResultType(Long.class);
    }

    /**
     * 检查是否允许通过
     * @param ip 用户 IP
     * @return true: 允许  false: 拒绝
     */
    public boolean isAllowed(String ip) {
        String key = IP_LIMIT_KEY_PREFIX + ip;
        Long result = redisTemplate.execute(
                limitScript,
                List.of(key),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(burst),
                String.valueOf(rate)
        );
        return result != null && result == 1;
    }
}