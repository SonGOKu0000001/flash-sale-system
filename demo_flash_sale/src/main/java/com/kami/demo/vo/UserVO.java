package com.kami.demo.vo;

import lombok.Data;

/**
 * @author kami
 * @description 用户信息返回对象，仅暴露 id 与 username，避免泄露密码等敏感字段
 */
@Data
public class UserVO {
    private Long id;
    private String username;
}