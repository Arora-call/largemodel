/**
 * 模块：用户体系
 * 功能：认证控制器，处理注册、登录、获取当前用户等公开接口
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.annotation.LogRecord;
import org.example.dto.request.ForgetPasswordRequest;
import org.example.dto.request.LoginRequest;
import org.example.dto.request.RegisterRequest;
import org.example.dto.response.ApiResponse;
import org.example.dto.response.LoginResponse;
import org.example.dto.response.UserInfoResponse;
import org.example.entity.User;
import org.example.auth.service.AuthService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     */
    @LogRecord(module = "auth", action = "REGISTER", target = "User")
    @PostMapping("/register")
    public ApiResponse<UserInfoResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserInfoResponse user = authService.register(request);
        return ApiResponse.success("注册成功", user);
    }

    /**
     * 用户登录
     */
    @LogRecord(module = "auth", action = "LOGIN", target = "User")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success("登录成功", response);
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserInfoResponse> getCurrentUser(@AuthenticationPrincipal User user) {
        UserInfoResponse userInfo = authService.getCurrentUser(user.getId());
        return ApiResponse.success(userInfo);
    }

    /**
     * 找回密码：通过用户名+邮箱验证身份后重置密码
     */
    @LogRecord(module = "auth", action = "RESET_PASSWORD", target = "User")
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgetPasswordRequest request) {
        authService.forgetPassword(request);
        return ApiResponse.msg("密码重置成功，请使用新密码登录");
    }
}
