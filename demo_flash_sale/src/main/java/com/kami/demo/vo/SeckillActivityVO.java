package com.kami.demo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author kami
 * @description 活动详情返回对象，包含活动基本信息、关联商品名称及 Redis 中的实时剩余库存
 */
@Data
public class SeckillActivityVO {
    private Long id;
    private String activityName;
    private String goodsName;
    private BigDecimal seckillPrice;
    private Integer stock;
    private Date startTime;
    private Date endTime;
    private Integer status;
}