package com.leaf.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leaf.common.result.Result;
import com.leaf.order.entity.Order;
import com.leaf.order.feign.UserClient;
import com.leaf.order.mapper.OrderMapper;
import com.leaf.order.service.OrderService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final UserClient userClient;
    private final OrderMapper orderMapper;

    @GlobalTransactional(name = "create-order-tx", rollbackFor = Exception.class)
    @Override
    public String createOrderWithTx(Long userId) {
        log.info("[Order Service] Starting global transaction. XID: {}", io.seata.core.context.RootContext.getXID());

        // 1. 本地扣减/预备一些本地事务...（本示例重点演示跨库）
        
        // 2. 远程调用 User 服务，扣减 100 元
        BigDecimal amount = new BigDecimal("100.00");
        Result<String> deductResult = userClient.deductBalance(userId, amount);
        
        if (deductResult.getCode() != 200) {
            // 如果调用用户服务降级或失败，直接抛出异常让 Seata 全局回滚
            throw new RuntimeException("Deduct balance failed, rolling back global transaction. Msg: " + deductResult.getMessage());
        }
        
        // 3. 往刚才建好的 t_order 表中插入订单
        Order order = new Order();
        order.setUserId(userId);
        order.setProductId(1001L);
        order.setCount(1);
        order.setMoney(amount);
        order.setStatus(0);
        orderMapper.insert(order);
        
        log.info("[Order Service] Order created locally. OrderId={}", order.getId());

        // 4. 重点：模拟异常宕机情况。如果测试的 userId 是 6，则强行抛出异常破坏事务
        if (userId.equals(6L)) {
            log.error("[Order Service] Triggering simulated exception for userId: 6. Seata should rollback user balance.");
            throw new RuntimeException("Simulated specific exception to test Seata rollback!");
        }

        return "Order created successfully. TX Commited. OrderID: " + order.getId();
    }
}
