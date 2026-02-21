package com.leaf.user.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leaf.user.entity.User;
import com.leaf.user.mapper.UserMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    @Cacheable(value = "userCache", key = "#id")
    public User getUserWithCache(Long id) {
        System.out.println(">>> Cache missed. Querying database for user: " + id);
        return this.getById(id);
    }
    
    // 注意：如果是单体应用中需要通过 @Transactional，在分布式环境下通常由远端调用的本地开启 RM, Seata 会自动代理数据源
    public void deductBalance(Long userId, java.math.BigDecimal amount) {
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("User not found: " + userId);
        }
        if (user.getBalance() == null) {
            user.setBalance(new java.math.BigDecimal("1000.00")); // fallback
        }
        if (user.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance for user: " + userId);
        }
        user.setBalance(user.getBalance().subtract(amount));
        this.updateById(user);
    }
}
