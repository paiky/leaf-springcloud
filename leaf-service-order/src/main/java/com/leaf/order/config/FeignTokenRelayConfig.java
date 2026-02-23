package com.leaf.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 请求拦截器 —— JWT Token 中继透传
 *
 * 作用：当 leaf-service-order 通过 Feign 调用其他微服务（如 leaf-service-user）时，
 * 自动从当前 HTTP 请求上下文中提取 Authorization: Bearer <token> 头，
 * 并添加到 Feign 发出的下游请求中，保证内部 RPC 调用链路的身份安全。
 *
 * 原理：Spring MVC 通过 RequestContextHolder 以 ThreadLocal 绑定当前线程的请求对象，
 * Feign 同步调用时共享同一线程，因此可以安全读取。
 *
 * 注意：不要让此类同时 implements RequestInterceptor 并用 @Bean 返回 this，
 * 否则会有同名 Bean 双重注册冲突。此处改用 @Configuration + @Bean 方式注册匿名实现。
 */
@Configuration
public class FeignTokenRelayConfig {

    @Bean
    public RequestInterceptor feignTokenRelayInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                ServletRequestAttributes attributes =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    String authHeader = request.getHeader("Authorization");

                    // 只有当 Authorization 头存在且是 Bearer Token 时才透传
                    if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                        template.header("Authorization", authHeader);
                    }
                }
            }
        };
    }
}
