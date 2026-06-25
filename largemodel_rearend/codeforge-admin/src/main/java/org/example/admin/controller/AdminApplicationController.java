/**
 * 模块：系统管理 — 应用管理
 * 功能：管理员查看/修改/删除所有用户的应用
 * 作者：yx
 * 创建时间：2026-06-25
 */
package org.example.admin.controller;

import lombok.RequiredArgsConstructor;
import org.example.admin.service.AdminApplicationService;
import org.example.annotation.AuthCheck;
import org.example.annotation.LogRecord;
import org.example.dto.response.ApiResponse;
import org.example.dto.response.PageResponse;
import org.example.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/applications")
@RequiredArgsConstructor
@AuthCheck("ADMIN")
public class AdminApplicationController {

    private final AdminApplicationService adminAppService;

    /** 管理员查看所有用户的应用 */
    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type) {

        Page<Application> apps = adminAppService.listAll(page, size, keyword, type);

        // 批量查询用户信息
        Set<Long> userIds = apps.getContent().stream()
                .map(Application::getUserId).collect(Collectors.toSet());
        Map<Long, String> userNames = adminAppService.getUserNameMap(userIds);

        List<Map<String, Object>> content = apps.getContent().stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId()); m.put("name", a.getName());
            m.put("description", a.getDescription() != null
                    ? a.getDescription().substring(0, Math.min(100, a.getDescription().length())) : "");
            m.put("type", a.getType()); m.put("language", a.getLanguage() != null ? a.getLanguage() : "");
            m.put("status", a.getStatus());
            m.put("priority", a.getPriority() != null ? a.getPriority() : 0);
            m.put("coverImage", a.getCoverImage() != null ? a.getCoverImage() : "");
            m.put("userId", a.getUserId());
            m.put("userName", userNames.getOrDefault(a.getUserId(), "用户" + a.getUserId()));
            m.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");
            m.put("updatedAt", a.getUpdatedAt() != null ? a.getUpdatedAt().toString() : "");
            return m;
        }).toList();

        return ApiResponse.success(PageResponse.<Map<String, Object>>builder()
                .content(content).totalElements(apps.getTotalElements())
                .totalPages(apps.getTotalPages()).currentPage(apps.getNumber())
                .pageSize(apps.getSize()).first(apps.isFirst()).last(apps.isLast()).build());
    }

    /** 管理员更新应用 */
    @LogRecord(module = "admin", action = "UPDATE", target = "Application")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        adminAppService.update(id,
                body.get("name"),
                body.get("description"),
                body.get("type"),
                body.get("language"));
        return ApiResponse.msg("已更新");
    }

    /** 管理员删除应用 */
    @LogRecord(module = "admin", action = "DELETE", target = "Application")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminAppService.delete(id);
        return ApiResponse.msg("已删除");
    }
}
