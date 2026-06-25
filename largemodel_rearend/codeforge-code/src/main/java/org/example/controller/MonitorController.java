/**
 * 模块：监控统计（用户端）
 * 功能：当前用户的 API 调用统计，数据隔离 — 用户只能看到自己的数据
 * 作者：yx
 * 创建时间：2026-06-25
 */
package org.example.controller;

import org.example.dto.response.ApiResponse;
import org.example.entity.User;
import org.example.service.MonitorService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(@AuthenticationPrincipal User user) {
        return ApiResponse.success(monitorService.overviewForUser(user.getId()));
    }

    @GetMapping("/calls")
    public ApiResponse<List<Map<String, Object>>> calls(
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal User user) {
        return ApiResponse.success(monitorService.callTrendForUser(days, user.getId()));
    }

    @GetMapping("/tokens")
    public ApiResponse<List<Map<String, Object>>> tokens(
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal User user) {
        return ApiResponse.success(monitorService.tokenTrendForUser(days, user.getId()));
    }

    @GetMapping("/models")
    public ApiResponse<List<Map<String, Object>>> models(@AuthenticationPrincipal User user) {
        return ApiResponse.success(monitorService.modelDistForUser(user.getId()));
    }
}
