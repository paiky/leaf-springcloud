package com.leaf.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    public SecurityConfig() {
        System.out.println(">>> [GATEWAY] Loading SecurityConfig Bean...");
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .authorizeExchange(exchanges -> exchanges
                // 允许放行的接口，比如登录认证本身
                .pathMatchers("/api/auth/**", "/oauth2/**", "/login").permitAll()
                // 微服务 Actuator 监控端点放行
                .pathMatchers("/actuator/**").permitAll()
                // 其他任何业务请求都需要带上合法的 Jwt Token
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            )
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable);
        return http.build();
    }
}
