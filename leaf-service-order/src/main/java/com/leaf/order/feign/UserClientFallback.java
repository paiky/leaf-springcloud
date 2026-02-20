package com.leaf.order.feign;

import com.leaf.common.result.Result;
import org.springframework.stereotype.Component;

/**
 * Sentinel 触发熔断时的降级逻辑
 */
@Component
public class UserClientFallback implements UserClient {
    @Override
    public Result<Object> getUserById(Long id) {
        return Result.fail(500, "User Service is down or timed out. Triggered by Sentinel Fallback.");
    }
}
