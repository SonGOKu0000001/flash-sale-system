package com.kami.demo.controller;

import com.kami.demo.common.Result;
import com.kami.demo.service.SeckillStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author kami
 * @createDate 2026-08-07 19:05
 * @description 测试接口
 */
@RestController
public class testController {
    //测试SeckillStockService
//    @Autowired
//    private SeckillStockService seckillStockService;
//    @GetMapping("/test/init")
//    public Result<String> init(){
//        seckillStockService.initStock(1L, 100);
//        return Result.success("初始化成功");
//    }
//    @GetMapping("/test/deduct")
//    public Result<String> test(){
//        Long result = seckillStockService.deductStock(1L, 1);
//        if(result == -1){
//            return Result.fail("库存未初始化");
//        }else if(result == 0){
//            return Result.fail("库存不足");
//        }
//        if(result == 1){
//            return Result.success("扣减成功");
//        }
//        return Result.fail("未知错误");
//    }
//    @GetMapping("/test/get")
//    public Result<Integer> get(){
//        return Result.success(seckillStockService.getStock(1L));
//    }
}
