/**
 * 模块：系统管理
 * 功能：管理员控制器，处理用户列表查询、角色/状态管理、用户删除等后台接口
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.ApiResponse;
import org.example.dto.response.PageResponse;
import org.example.dto.response.UserInfoResponse;
import org.example.entity.User;
import org.example.service.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /**
     * 用户列表（分页）
     */
    @GetMapping("/users")
    public ApiResponse<PageResponse<UserInfoResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(adminService.listUsers(page, size, keyword, role, status));
    }

    /**
     * 用户详情
     */
    @GetMapping("/users/{userId}")
    public ApiResponse<UserInfoResponse> getUserDetail(@PathVariable Long userId) {
        return ApiResponse.success(adminService.getUserDetail(userId));
    }

    /**
     * 修改用户状态（启用/禁用）
     */
    @PutMapping("/users/{userId}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long userId,
                                               @RequestParam Integer status) {
        adminService.updateUserStatus(userId, status);
        return ApiResponse.msg("状态更新成功");
    }

    /**
     * 修改用户角色
     */
    @PutMapping("/users/{userId}/role")
    public ApiResponse<Void> updateUserRole(@PathVariable Long userId,
                                             @RequestParam String role) {
        adminService.updateUserRole(userId, role);
        return ApiResponse.msg("角色更新成功");
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId,
                                         @AuthenticationPrincipal User admin) {
        adminService.deleteUser(userId, admin.getId());
        return ApiResponse.msg("用户已删除");
    }

    /**
     * 重置用户密码
     */
    @PutMapping("/users/{userId}/password")
    public ApiResponse<Void> resetUserPassword(@PathVariable Long userId,
                                                @RequestParam String newPassword) {
        adminService.resetUserPassword(userId, newPassword);
        return ApiResponse.msg("密码已重置");
    }

    /**
     * 统计数据概览
     */
    @GetMapping("/stats")
    public ApiResponse<java.util.Map<String, Object>> getStats() {
        long totalUsers = adminService.getUserCount();
        return ApiResponse.success(java.util.Map.of("totalUsers", totalUsers));
    }
}
