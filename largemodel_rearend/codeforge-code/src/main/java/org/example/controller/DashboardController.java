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
import org.example.repository.ConversationRepository;
import org.example.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ApplicationRepository appRepo;
    private final UserRepository userRepo;
    private final ConversationRepository convRepo;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats(@AuthenticationPrincipal User user) {
        // 统计当前用户所有类型（NATIVE + ENGINEERING）的活跃对话
        long totalConvs = convRepo.countByUserIdAndStatus(user.getId(), 1);
        long totalApps = appRepo.countByUserIdAndStatusNot(user.getId(), 0);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalApps", totalApps);
        data.put("totalConversations", totalConvs);
        data.put("role", user.getRole().name());
        if (user.isAdmin()) {
            data.put("totalUsers", userRepo.countByDeletedFalse());
        }
        return ApiResponse.success(data);
    }
}
