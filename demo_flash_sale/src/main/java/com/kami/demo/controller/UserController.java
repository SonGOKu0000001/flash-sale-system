package com.kami.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kami.demo.common.Result;
import com.kami.demo.entities.TUser;
import com.kami.demo.mapper.TUserMapper;
import com.kami.demo.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author kami
 * @description 用户接口控制器，提供用户列表查询，供前端登录下拉框选择用户
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private TUserMapper userMapper;

    @GetMapping("/list")
    public Result<List<UserVO>> list() {
        List<TUser> users = userMapper.selectList(
                new LambdaQueryWrapper<TUser>().eq(TUser::getStatus, 1)
        );
        List<UserVO> vos = users.stream().map(user -> {
            UserVO vo = new UserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            return vo;
        }).collect(Collectors.toList());
        return Result.success(vos);
    }
}