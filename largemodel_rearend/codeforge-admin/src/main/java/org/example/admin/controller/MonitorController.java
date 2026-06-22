/**
 * 监控统计 API — admin 模块
 */
package org.example.admin.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.ApiResponse;
import org.example.service.MonitorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.success(monitorService.overview());
    }

    @GetMapping("/calls")
    public ApiResponse<List<Map<String, Object>>> calls(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(monitorService.callTrend(days));
    }

    @GetMapping("/tokens")
    public ApiResponse<List<Map<String, Object>>> tokens(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(monitorService.tokenTrend(days));
    }

    @GetMapping("/models")
    public ApiResponse<List<Map<String, Object>>> models() {
        return ApiResponse.success(monitorService.modelDist());
    }
}
