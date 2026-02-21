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
        User user = userService.getUserWithCache(id);
        if (user != null) {
            // 屏蔽密码返回
            user.setPassword(null);
        }
        return Result.success(user);
    }
    
    @PostMapping("/deductBalance")
    public Result<String> deductBalance(@RequestParam("userId") Long userId, 
                                        @RequestParam("amount") java.math.BigDecimal amount) {
        try {
            userService.deductBalance(userId, amount);
            return Result.success("Success deducted " + amount + " from user " + userId);
        } catch (Exception e) {
            return Result.fail(500, e.getMessage());
        }
    }
}
