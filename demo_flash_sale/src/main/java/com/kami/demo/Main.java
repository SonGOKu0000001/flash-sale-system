package com.kami.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author kami
 * @description 限时抢购系统启动类，负责 Spring Boot 应用启动以及 Mapper 接口扫描
 */
@SpringBootApplication
@MapperScan("com.kami.demo.mapper")
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}