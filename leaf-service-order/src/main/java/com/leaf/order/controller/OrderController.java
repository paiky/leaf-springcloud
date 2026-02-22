package com.leaf.order.controller;

import com.leaf.common.result.Result;
import com.leaf.order.feign.UserClient;
import com.leaf.order.service.OrderService;
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
    private final OrderService orderService; // 注入 OrderService

    @GetMapping("/create/{userId}")
    public Result<Object> createOrder(@PathVariable("userId") Long userId) {
        
        try {
            // 通过有 @GlobalTransactional 保护的 Service 统筹调用链路和本地强逻辑
            String orderInfo = orderService.createOrderWithTx(userId);
            
            // 3. 异步发送订单创建MQ消息 (原逻辑保留，此操作不在事务块内强制绑定)
            rocketMQTemplate.convertAndSend("order-created-topic", "New Order for userId: " + userId);
            
            return Result.success(orderInfo);
        } catch (Exception e) {
            log.error("[Order Controller] createOrder failed", e);
            return Result.fail(500, "Transaction Failed and Rolled Back: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<Object> getOrderById(@PathVariable("id") Long id) {
        // Find order by ID from DB and return it.
        com.leaf.order.entity.Order order = orderService.getById(id);
        if (order != null) {
            return Result.success(order);
        } else {
            return Result.fail(404, "Order not found");
        }
    }
}
