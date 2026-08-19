package com.kami.demo.controller;

import com.kami.demo.common.Result;
import com.kami.demo.common.SeckillResult;
import com.kami.demo.service.SeckillService;
import com.kami.demo.vo.SeckillActivityVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author kami
 * @description 秒杀接口控制器，提供活动详情查询、发起抢购、查询抢购结果三个接口
 */
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @PostMapping("/{activityId}")
    public Result<String> seckill(@PathVariable Long activityId,
                                  HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        SeckillResult result = seckillService.seckill(userId, activityId);
        if (result.isSuccess()) {
            return Result.success(result.getMessage());
        }
        return Result.fail(result.getMessage());
    }

    @GetMapping("/result/{activityId}")
    public Result<String> result(@PathVariable Long activityId,
                                 HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        SeckillResult result = seckillService.queryResult(userId, activityId);
        return Result.success(result.getMessage());
    }

    @GetMapping("/activity/list")
    public Result<List<SeckillActivityVO>> activityList() {
        return Result.success(seckillService.getActivityList());
    }

    @GetMapping("/activity/{activityId}")
    public Result<SeckillActivityVO> getActivity(@PathVariable Long activityId) {
        SeckillActivityVO vo = seckillService.getActivityWithStock(activityId);
        return Result.success(vo);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isEmpty()) {
            return 1L;
        }
        return Long.parseLong(userId);
    }
}