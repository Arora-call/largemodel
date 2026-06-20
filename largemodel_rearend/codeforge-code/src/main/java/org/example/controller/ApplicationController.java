/**
 * 模块：应用管理
 * 功能：应用控制器，处理应用保存/列表/详情/删除/下载接口
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.ApiResponse;
import org.example.dto.response.PageResponse;
import org.example.entity.Application;
import org.example.entity.User;
import org.example.service.ApplicationService;
import org.example.service.ProjectService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService appService;
    private final ProjectService projectService;

    /** 保存应用 */
    @PostMapping
    public ApiResponse<Map<String, Object>> save(@RequestBody Map<String, Object> body,
                                                  @AuthenticationPrincipal User user) {
        Long id = body.get("id") != null ? ((Number) body.get("id")).longValue() : null;
        String name = (String) body.getOrDefault("name", "未命名");
        String desc = (String) body.getOrDefault("description", "");
        String type = (String) body.getOrDefault("type", "NATIVE");
        String lang = (String) body.getOrDefault("language", "");
        String code = (String) body.getOrDefault("sourceCode", "");
        String config = (String) body.getOrDefault("configJson", "");

        Application app = appService.saveOrUpdate(id, name, desc, type, lang, user.getId(), code, config);
        return ApiResponse.success(Map.of("id", app.getId(), "name", app.getName()));
    }

    /** 应用列表 */
    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String language) {

        Page<Application> apps = appService.listByUser(user.getId(), page, size, keyword, language);
        List<Map<String, Object>> content = apps.getContent().stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId()); m.put("name", a.getName());
            m.put("description", a.getDescription() != null ? a.getDescription().substring(0, Math.min(100, a.getDescription().length())) : "");
            m.put("type", a.getType()); m.put("language", a.getLanguage() != null ? a.getLanguage() : "");
            m.put("status", a.getStatus()); m.put("coverImage", a.getCoverImage() != null ? a.getCoverImage() : "");
            m.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");
            m.put("updatedAt", a.getUpdatedAt() != null ? a.getUpdatedAt().toString() : "");
            return m;
        }).toList();

        return ApiResponse.success(PageResponse.<Map<String, Object>>builder()
                .content(content).totalElements(apps.getTotalElements())
                .totalPages(apps.getTotalPages()).currentPage(apps.getNumber())
                .pageSize(apps.getSize()).first(apps.isFirst()).last(apps.isLast()).build());
    }

    /** 应用详情 */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id,
                                                    @AuthenticationPrincipal User user) {
        Application a = appService.getById(id, user.getId());
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", a.getId()); detail.put("name", a.getName());
        detail.put("description", a.getDescription()); detail.put("type", a.getType());
        detail.put("language", a.getLanguage() != null ? a.getLanguage() : "");
        detail.put("status", a.getStatus());
        detail.put("sourceCode", a.getSourceCode() != null ? a.getSourceCode() : "");
        detail.put("configJson", a.getConfigJson() != null ? a.getConfigJson() : "");
        detail.put("coverImage", a.getCoverImage() != null ? a.getCoverImage() : "");
        detail.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");
        detail.put("updatedAt", a.getUpdatedAt() != null ? a.getUpdatedAt().toString() : "");
        return ApiResponse.success(detail);
    }

    /** 删除应用 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        appService.delete(id, user.getId());
        return ApiResponse.msg("已删除");
    }

    /** 下载代码（工程项目用磁盘 ZIP，原生应用用数据库源码） */
    @GetMapping("/{id}/download")
    public ResponseEntity<?> download(@PathVariable Long id, @AuthenticationPrincipal User user) {
        Application a = appService.getById(id, user.getId());
        String fileName = (a.getName() != null ? a.getName() : "app") + ".zip";

        byte[] data;
        // 工程项目：从磁盘文件打包 ZIP
        if ("ENGINEERING".equalsIgnoreCase(a.getType())) {
            try {
                java.nio.file.Path zipPath = projectService.createZip(id);
                data = java.nio.file.Files.readAllBytes(zipPath);
            } catch (Exception e) {
                throw new org.example.exception.BusinessException("项目文件打包失败");
            }
        } else {
            // 原生应用：从数据库源码生成下载
            data = appService.downloadCode(id, user.getId());
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
