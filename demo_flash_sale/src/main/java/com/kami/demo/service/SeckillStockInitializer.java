package com.kami.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kami.demo.entities.TSeckillActivity;
import com.kami.demo.mapper.TSeckillActivityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author kami
 * @description 应用启动时自动将进行中活动的库存预热到 Redis，避免因库存未初始化导致抢购返回"活动未初始化"
 */
@Slf4j
@Component
public class SeckillStockInitializer implements CommandLineRunner {

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";

    @Autowired
    private TSeckillActivityMapper activityMapper;

    @Autowired
    private SeckillStockService stockService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void run(String... args) {
        List<TSeckillActivity> activities = activityMapper.selectList(
                new LambdaQueryWrapper<TSeckillActivity>()
                        .eq(TSeckillActivity::getStatus, 1)
        );
        for (TSeckillActivity activity : activities) {
            String key = STOCK_KEY_PREFIX + activity.getId();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                log.info("活动 {} 库存已存在，跳过初始化", activity.getId());
                continue;
            }
            stockService.initStock(activity.getId(), activity.getStockCount(), activity.getEndTime());
            log.info("活动 {} 库存已初始化到 Redis：{}", activity.getId(), activity.getStockCount());
        }
        log.info("活动库存初始化完成，共处理 {} 个进行中活动", activities.size());
    }
}