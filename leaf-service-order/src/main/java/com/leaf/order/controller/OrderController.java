package com.leaf.order.controller;

import com.leaf.common.result.Result;
import com.leaf.order.feign.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final UserClient userClient;
    private final RocketMQTemplate rocketMQTemplate;

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
        
        // 3. 发送订单创建MQ消息 (供下游如短信/物流服务消费)
        try {
            rocketMQTemplate.convertAndSend("order-created-topic", "New Order for userId: " + userId);
            log.info("Successfully sent order event to RocketMQ for userId: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send order event to RocketMQ", e);
        }

        return Result.success(orderInfo);
    }
}
