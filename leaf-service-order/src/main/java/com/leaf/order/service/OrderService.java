package com.leaf.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.leaf.order.entity.Order;

public interface OrderService extends IService<Order> {

    /**
     * 创建订单，并演示分布式事务
     *
     * @param userId
     * @return
     */
    String createOrderWithTx(Long userId);
}
