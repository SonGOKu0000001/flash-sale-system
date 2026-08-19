package com.kami.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.hutool.core.util.IdUtil;
import com.kami.demo.config.SeckillProperties;
import com.kami.demo.dto.SeckillMessage;
import com.kami.demo.entities.TOrder;
import com.kami.demo.entities.TSeckillActivity;
import com.kami.demo.exception.StockInsufficientException;
import com.kami.demo.mapper.TOrderMapper;
import com.kami.demo.mapper.TSeckillActivityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * @author kami
 * @description 订单创建服务，消费秒杀消息后幂等校验、扣减 MySQL 活动库存、生成订单号写入订单表，并更新抢购结果缓存供用户轮询
 */
@Slf4j
@Service
public class OrderService {

    private static final String SECKILL_RESULT_KEY = "seckill:result:";

    @Autowired
    private TOrderMapper orderMapper;

    @Autowired
    private TSeckillActivityMapper activityMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SeckillProperties seckillProperties;

    @Transactional
    public void createOrder(SeckillMessage message) {
        // 0. 幂等查重：存在有效订单（status != 2 未取消）则直接跳过，支持取消后重新抢购
        Long count = orderMapper.selectCount(new LambdaQueryWrapper<TOrder>()
                .eq(TOrder::getUserId, message.getUserId())
                .eq(TOrder::getActivityId, message.getActivityId())
                .ne(TOrder::getStatus, 2));
        if (count != null && count > 0) {
            log.info("重复下单消息，跳过：用户 {} 活动 {}", message.getUserId(), message.getActivityId());
            return;
        }

        // 1. 扣减 MySQL 活动库存，限制库存大于 0 防止库存为负
        LambdaUpdateWrapper<TSeckillActivity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(TSeckillActivity::getId, message.getActivityId())
                .gt(TSeckillActivity::getStockCount, 0)
                .setSql("stock_count = stock_count - 1");
        int rows = activityMapper.update(null, updateWrapper);
        if (rows == 0) {
            throw new StockInsufficientException("活动库存不足");
        }

        // 2. 生成订单号并写入订单表
        String orderNo = generateOrderNo();
        TOrder order = new TOrder();
        order.setOrderNo(orderNo);
        order.setUserId(message.getUserId());
        order.setActivityId(message.getActivityId());
        order.setGoodsId(message.getGoodsId());
        order.setSeckillPrice(message.getSeckillPrice());
        order.setQuantity(1);
        order.setTotalAmount(message.getSeckillPrice());
        order.setStatus(0);
        orderMapper.insert(order);

        // 3. 更新抢购结果缓存，供用户轮询
        String key = SECKILL_RESULT_KEY + message.getActivityId() + ":" + message.getUserId();
        redisTemplate.opsForValue().set(
                key,
                "订单创建成功，订单号：" + orderNo,
                Duration.ofSeconds(seckillProperties.getExpire().getResultTtl())
        );
    }

    private String generateOrderNo() {
        return "SK" + IdUtil.getSnowflakeNextId();
    }
}