package com.kami.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kami.demo.common.SeckillResult;
import com.kami.demo.config.SeckillProperties;
import com.kami.demo.dto.SeckillMessage;
import com.kami.demo.entities.TGoods;
import com.kami.demo.entities.TSeckillActivity;
import com.kami.demo.mapper.TGoodsMapper;
import com.kami.demo.mapper.TSeckillActivityMapper;
import com.kami.demo.vo.SeckillActivityVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author kami
 * @description 秒杀核心业务编排服务，串联活动校验、防重复抢购、分布式锁、Redis 预减库存、发送 MQ 消息的完整抢购链路
 */
@Service
public class SeckillService {

    private static final String SECKILL_RESULT_KEY = "seckill:result:";
    private static final String SECKILL_RECORD_KEY = "seckill:record:";

    @Autowired
    private SeckillStockService stockService;

    @Autowired
    private SeckillLockService lockService;

    @Autowired
    private SeckillMessageProducer messageProducer;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private TSeckillActivityMapper activityMapper;

    @Autowired
    private TGoodsMapper goodsMapper;

    @Autowired
    private SeckillProperties seckillProperties;

    public SeckillResult seckill(Long userId, Long activityId) {
        // 1. 检查活动状态
        TSeckillActivity activity = activityMapper.selectById(activityId);
        if (activity == null || !isValidActivity(activity)) {
            return SeckillResult.fail("活动未开始或已结束");
        }

        // 2. 获取分布式锁
        if (!lockService.tryLock(userId, activityId)) {
            return SeckillResult.fail("系统繁忙，请稍后再试");
        }

        try {
            // 3. 锁内检查是否已抢购（并发安全的防重复检查）
            if (hasSecKillRecord(userId, activityId)) {
                return SeckillResult.fail("您已参与过该活动");
            }

            // 4. 预减库存
            Long result = stockService.deductStock(activityId, 1);

            if (result == -1) return SeckillResult.fail("活动未初始化");
            if (result == 0) return SeckillResult.fail("商品已售罄");

            // 5. 设置已抢购标记（活动结束+1天过期）
            setSecKillRecord(userId, activityId, activity.getEndTime());

            // 6. 发送 MQ 异步下单
            SeckillMessage message = new SeckillMessage(
                    userId, activityId,
                    activity.getGoodsId(),
                    activity.getSeckillPrice(),
                    System.currentTimeMillis()
            );
            messageProducer.sendSeckillMessage(message);

            // 7. 缓存结果
            cacheResult(userId, activityId, "抢购成功，正在生成订单");
            return SeckillResult.success("抢购成功");

        } finally {
            lockService.unlock(userId, activityId);
        }
    }

    public SeckillResult queryResult(Long userId, Long activityId) {
        String key = SECKILL_RESULT_KEY + activityId + ":" + userId;
        String result = redisTemplate.opsForValue().get(key);
        if (result != null) {
            return SeckillResult.success(result);
        }
        return SeckillResult.fail("结果生成中，请稍候");
    }

    public SeckillActivityVO getActivityWithStock(Long activityId) {
        TSeckillActivity activity = activityMapper.selectById(activityId);
        return activity == null ? null : toVO(activity);
    }

    /**
     * 查询全部活动列表（含未开始/进行中/已结束），按开始时间升序
     */
    public List<SeckillActivityVO> getActivityList() {
        List<TSeckillActivity> activities = activityMapper.selectList(
                new LambdaQueryWrapper<TSeckillActivity>()
                        .orderByAsc(TSeckillActivity::getStartTime)
        );
        return activities.stream().map(this::toVO).collect(Collectors.toList());
    }

    private SeckillActivityVO toVO(TSeckillActivity activity) {
        TGoods goods = goodsMapper.selectById(activity.getGoodsId());
        SeckillActivityVO vo = new SeckillActivityVO();
        vo.setId(activity.getId());
        vo.setActivityName(activity.getActivityName());
        vo.setGoodsName(goods == null ? null : goods.getGoodsName());
        vo.setSeckillPrice(activity.getSeckillPrice());
        vo.setStock(stockService.getStock(activity.getId()));
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setStatus(activity.getStatus());
        return vo;
    }

    private boolean hasSecKillRecord(Long userId, Long activityId) {
        String key = SECKILL_RECORD_KEY + activityId + ":" + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private void setSecKillRecord(Long userId, Long activityId, Date endTime) {
        String key = SECKILL_RECORD_KEY + activityId + ":" + userId;
        long seconds = endTime == null
                ? seckillProperties.getExpire().getResultTtl()
                : (endTime.getTime() + 86_400_000L - System.currentTimeMillis()) / 1000;
        redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(Math.max(seconds, 60)));
    }

    private void cacheResult(Long userId, Long activityId, String msg) {
        String key = SECKILL_RESULT_KEY + activityId + ":" + userId;
        redisTemplate.opsForValue().set(key, msg,
                Duration.ofSeconds(seckillProperties.getExpire().getResultTtl()));
    }

    private boolean isValidActivity(TSeckillActivity activity) {
        Date now = new Date();
        return activity.getStatus() == 1
                && now.after(activity.getStartTime())
                && now.before(activity.getEndTime());
    }
}