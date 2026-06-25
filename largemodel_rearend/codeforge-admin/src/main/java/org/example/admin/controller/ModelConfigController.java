/**
 * AI 模型配置管理接口 — 仅管理员可访问
 */
package org.example.admin.controller;

import lombok.RequiredArgsConstructor;
import org.example.annotation.AuthCheck;
import org.example.annotation.LogRecord;
import org.example.dto.response.ApiResponse;
import org.example.entity.ModelConfig;
import org.example.admin.service.ModelConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/models")
@RequiredArgsConstructor
public class ModelConfigController {

    private final ModelConfigService service;

    /** 列出全部模型（管理用） */
    @AuthCheck("ADMIN")
    @GetMapping
    public ApiResponse<List<ModelConfig>> list() {
        return ApiResponse.success(service.listAll());
    }

    /** 单个详情 */
    @AuthCheck("ADMIN")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    /** 添加模型 */
    @AuthCheck("ADMIN")
    @LogRecord(module = "admin", action = "CREATE", target = "ModelConfig")
    @PostMapping
    public ApiResponse<ModelConfig> create(@RequestBody Map<String, Object> body) {
        ModelConfig mc = service.create(
                (String) body.get("name"),
                (String) body.get("provider"),
                (String) body.get("baseUrl"),
                (String) body.get("apiKey"),
                (String) body.get("modelName"),
                body.get("temperature") != null ? ((Number) body.get("temperature")).doubleValue() : null,
                body.get("maxTokens") != null ? ((Number) body.get("maxTokens")).intValue() : null,
                body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : null
        );
        return ApiResponse.success(mc);
    }

    /** 更新模型 */
    @AuthCheck("ADMIN")
    @LogRecord(module = "admin", action = "UPDATE", target = "ModelConfig")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        service.update(id,
                (String) body.get("name"),
                (String) body.get("provider"),
                (String) body.get("baseUrl"),
                (String) body.get("apiKey"),
                (String) body.get("modelName"),
                body.get("temperature") != null ? ((Number) body.get("temperature")).doubleValue() : null,
                body.get("maxTokens") != null ? ((Number) body.get("maxTokens")).intValue() : null,
                body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : null,
                body.get("isEnabled") != null ? ((Number) body.get("isEnabled")).intValue() : null,
                body.get("isDefault") != null ? ((Number) body.get("isDefault")).intValue() : null
        );
        return ApiResponse.success(null);
    }

    /** 删除模型 */
    @AuthCheck("ADMIN")
    @LogRecord(module = "admin", action = "DELETE", target = "ModelConfig")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success(null);
    }

    /** 测试连接 */
    @AuthCheck("ADMIN")
    @PostMapping("/{id}/test")
    public ApiResponse<Map<String, Object>> test(@PathVariable Long id) {
        return ApiResponse.success(service.testConnection(id));
    }

    /** 列出已启用模型（前端 AI 对话页调用） */
    @GetMapping("/enabled")
    public ApiResponse<List<Map<String, Object>>> listEnabled() {
        return ApiResponse.success(service.listEnabled());
    }
}
