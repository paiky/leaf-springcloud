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
}
