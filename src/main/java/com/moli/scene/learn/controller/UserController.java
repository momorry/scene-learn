package com.moli.scene.learn.controller;

import com.moli.scene.learn.service.RabbitMqUserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    @Resource
    private RabbitMqUserService rabbitMqUserService;

    @DeleteMapping("/update-user/{userId}")
    public Object updateUserRabbitMq(@PathVariable("userId") Long userId) {
        rabbitMqUserService.updateUserMq(userId, "张三");
        return "ok";
    }

    @GetMapping("/user")
    public Object queryById(@RequestParam("userId") Long userId) {
        return rabbitMqUserService.queryUserById(userId);
    }

}
