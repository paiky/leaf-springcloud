package com.leaf.user.controller;

import com.leaf.common.result.Result;
import com.leaf.user.entity.User;
import com.leaf.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable("id") Long id) {
        User user = userService.getById(id);
        if (user != null) {
            // 屏蔽密码返回
            user.setPassword(null);
        }
        return Result.success(user);
    }
}
