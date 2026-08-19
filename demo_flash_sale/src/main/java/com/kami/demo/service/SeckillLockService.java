package com.kami.demo.service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author kami
 * @description Redisson 分布式锁服务，以用户ID+活动ID作为锁 key，保证同一用户在同一个活动中只能抢购一次
 */
@Service
public class SeckillLockService {

    private static final String LOCK_KEY_PREFIX = "seckill:lock:";

    @Autowired
    private RedissonClient redissonClient;

    @Value("${seckill.lock.wait-time:3}")
    private long waitTime;

    @Value("${seckill.lock.lease-time:10}")
    private long leaseTime;

    /**
     * 尝试获取抢购锁
     */
    public boolean tryLock(Long userId, Long activityId) {
        String key = LOCK_KEY_PREFIX + userId + ":" + activityId;
        RLock lock = redissonClient.getLock(key);
        try {
            return lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放抢购锁
     */
    public void unlock(Long userId, Long activityId) {
        String key = LOCK_KEY_PREFIX + userId + ":" + activityId;
        RLock lock = redissonClient.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}