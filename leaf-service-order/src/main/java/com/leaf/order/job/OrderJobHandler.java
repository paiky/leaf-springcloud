package com.leaf.order.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OrderJobHandler {
    private static Logger logger = LoggerFactory.getLogger(OrderJobHandler.class);

    /**
     * 演示：定时扫描超时未支付订单的处理任务
     */
    @XxlJob("timeoutOrderJobHandler")
    public void timeoutOrderJobHandler() throws Exception {
        // 通过 XxlJobHelper 打印执行日志，可在 XXL-JOB 调度中心控制台查看
        XxlJobHelper.log("XXL-JOB, Hello World. Starting to scan timeout orders.");
        logger.info(">>> Execute timeoutOrderJobHandler at: {}", LocalDateTime.now());

        try {
            // 模拟业务执行，比如：查询 t_order 状态为 0 (创建中) 的且超时的数据
            // List<Order> orders = orderMapper.selectList(...);
            int timeoutCount = 5; // 模拟查出来 5 条超时订单
            for (int i = 0; i < timeoutCount; i++) {
                XxlJobHelper.log("Handling timeout order ID: {}", 1000 + i);
                // 模拟处理耗时
                Thread.sleep(200);
            }

            // 执行成功
            XxlJobHelper.handleSuccess("Timeout orders successfully handled. Count: " + timeoutCount);
        } catch (Exception e) {
            XxlJobHelper.handleFail("Timeout order handling failed: " + e.getMessage());
            logger.error("Job Failed", e);
        }
    }
}
