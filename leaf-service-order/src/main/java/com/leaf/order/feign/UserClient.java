package com.leaf.order.feign;

import com.leaf.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// fallback 可以指向一个实现了该接口的降级兜底类，用于触发 Sentinel 熔断时返回默认数据
@FeignClient(name = "leaf-service-user", fallback = UserClientFallback.class)
public interface UserClient {

    @GetMapping("/user/{id}")
    Result<Object> getUserById(@PathVariable("id") Long id);
}
