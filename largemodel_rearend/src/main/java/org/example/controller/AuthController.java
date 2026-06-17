package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.LoginRequest;
import org.example.dto.request.RegisterRequest;
import org.example.dto.response.ApiResponse;
import org.example.dto.response.LoginResponse;
import org.example.dto.response.UserInfoResponse;
import org.example.entity.User;
import org.example.service.AuthService;
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
    @PostMapping("/register")
    public ApiResponse<UserInfoResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserInfoResponse user = authService.register(request);
        return ApiResponse.success("注册成功", user);
    }

    /**
     * 用户登录
     */
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
}
