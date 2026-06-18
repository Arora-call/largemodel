/**
 * 模块：用户体系
 * 功能：个人信息控制器，处理用户信息查看/修改、密码修改、账户注销等接口
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
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
import org.springframework.web.multipart.MultipartFile;

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
        return ApiResponse.msg("密码修改成功");
    }

    /**
     * 注销账户
     */
    @DeleteMapping("/account")
    public ApiResponse<Void> deleteAccount(@AuthenticationPrincipal User user) {
        userService.deleteAccount(user.getId());
        return ApiResponse.msg("账户已注销");
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public ApiResponse<String> uploadAvatar(@AuthenticationPrincipal User user,
                                             @RequestParam("file") MultipartFile file) {
        String avatarUrl = userService.uploadAvatar(user.getId(), file);
        return ApiResponse.success("头像上传成功", avatarUrl);
    }
}
