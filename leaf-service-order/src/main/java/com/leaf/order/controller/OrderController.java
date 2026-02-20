package com.leaf.order.controller;

import com.leaf.common.result.Result;
import com.leaf.order.feign.UserClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final UserClient userClient;

    @GetMapping("/create/{userId}")
    public Result<Object> createOrder(@PathVariable("userId") Long userId) {
        // 1. 通过 OpenFeign 远程调用 user 服务获取用户信息
        Result<Object> userResult = userClient.getUserById(userId);

        if (userResult.getCode() != 200) {
            // 如果返回非200（例如触发了Sentinel熔断），直接提示失败
            return Result.fail(userResult.getCode(), "Create order failed: " + userResult.getMessage());
        }

        // 2. 模拟订单创建逻辑
        String orderInfo = "Order successfully created for user: " + userResult.getData();
        return Result.success(orderInfo);
    }
}
