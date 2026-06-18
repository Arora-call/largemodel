/**
 * 模块：首页总览
 * 功能：Dashboard 统计数据接口
 * 作者：yx
 * 创建时间：2026-06-18
 */
package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.ApiResponse;
import org.example.entity.User;
import org.example.repository.ApplicationRepository;
import org.example.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ApplicationRepository appRepo;
    private final UserRepository userRepo;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats(@AuthenticationPrincipal User user) {
        long totalApps = appRepo.countByUserIdAndStatusNot(user.getId(), 0);
        long totalUsers = user.isAdmin() ? userRepo.countByDeletedFalse() : 0;
        return ApiResponse.success(Map.of(
                "totalApps", totalApps,
                "totalUsers", totalUsers,
                "role", user.getRole().name()
        ));
    }
}
