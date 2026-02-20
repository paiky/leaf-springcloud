package com.leaf.user.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "order-created-topic", consumerGroup = "leaf-service-user-group")
public class OrderCreatedListener implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        log.info(">>> [User Service Consumer] Received RocketMQ message: {}", message);
        // 这里可以执行诸如：给用户发短信、更新用户积分等业务逻辑
    }
}
