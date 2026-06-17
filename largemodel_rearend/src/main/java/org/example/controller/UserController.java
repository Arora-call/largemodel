package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.UserPasswordRequest;
import org.example.dto.request.UserUpdateRequest;
import org.example.dto.response.ApiResponse;
import org.example.dto.response.UserInfoResponse;
import org.example.entity.User;
import org.example.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final UserService userService;

    /**
     * 获取当前用户个人信息
     */
    @GetMapping("/info")
    public ApiResponse<UserInfoResponse> getUserInfo(@AuthenticationPrincipal User user) {
        return ApiResponse.success(userService.getUserInfo(user.getId()));
    }

    /**
     * 更新个人信息
     */
    @PutMapping("/info")
    public ApiResponse<UserInfoResponse> updateUser(@AuthenticationPrincipal User user,
                                                     @RequestBody UserUpdateRequest request) {
        return ApiResponse.success("更新成功", userService.updateUser(user.getId(), request));
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal User user,
                                            @RequestBody UserPasswordRequest request) {
        userService.changePassword(user.getId(), request);
        return ApiResponse.success("密码修改成功");
    }

    /**
     * 注销账户
     */
    @DeleteMapping("/account")
    public ApiResponse<Void> deleteAccount(@AuthenticationPrincipal User user) {
        userService.deleteAccount(user.getId());
        return ApiResponse.success("账户已注销");
    }
}
