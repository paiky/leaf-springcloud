package com.leaf.user.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leaf.user.entity.User;
import com.leaf.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {
}
