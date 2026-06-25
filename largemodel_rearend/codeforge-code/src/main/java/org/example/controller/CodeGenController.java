/**
 * 模块：AI代码生成
 * 功能：统一代码生成控制器，提供 SSE 流式代码生成接口（新架构）
 * 作者：yx
 * 创建时间：2026-06-24
 */
package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.request.CodeModifyRequest;
import org.example.dto.request.GenerateCodeRequest;
import org.example.entity.User;
import org.example.service.DeployService;
import org.example.service.ai.AiCodeGenService;
import org.example.service.ai.CodeGenerationExecutor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 统一 AI 工作台控制器。
 * <p>
 * 新架构使用 /api/codegen/stream 作为统一入口，
 * 由 {@link CodeGenerationExecutor} 根据 mode 字段路由到对应策略。
 * <p>
 * 旧端点 /api/ai/generate/stream 和 /api/projects/generate 保留兼容。
 */
@Slf4j
@RestController
@RequestMapping("/api/codegen")
@RequiredArgsConstructor
public class CodeGenController {

    private final CodeGenerationExecutor executor;
    private final AiCodeGenService aiCodeGenService; // 用于修改和审查（暂未迁移）
    private final DeployService deployService;

    /**
     * 统一 SSE 流式代码生成（新架构入口）。
     * <p>
     * 请求体中 mode 字段决定生成模式：
     * <ul>
     *   <li>SINGLE_FILE — 单文件 HTML（CSS/JS 内联）</li>
     *   <li>MULTI_FILE  — 多文件 HTML（分离 HTML/CSS/JS）</li>
     *   <li>VUE_PROJECT — Vue3 工程项目（完整 Vite 工程）</li>
     * </ul>
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateStream(@Valid @RequestBody GenerateCodeRequest request,
                                      @AuthenticationPrincipal User user) {
        return executor.execute(request, user.getId());
    }

    /**
     * SSE 流式代码修改（基于现有代码 + 选中元素 + 修改需求）。
     * 暂时委托给旧 AiCodeGenService，后续迁移到策略模式。
     */
    @PostMapping(value = "/modify/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter modifyStream(@RequestBody CodeModifyRequest request,
                                    @AuthenticationPrincipal User user) {
        return aiCodeGenService.modifyCodeStream(request, user.getId());
    }

    /**
     * 部署对话代码到 Nginx 预览目录。
     * <p>
     * 将对话中所有 project_files 复制到部署目录，生成 6 位 deployKey，
     * 返回可通过 Nginx 访问的 URL（如 http://localhost/abc123/）。
     * <p>
     * 部署后多文件项目（CSS/JS 分离）即可通过 Nginx 正常加载。
     */
    @PostMapping("/deploy")
    public ResponseEntity<Map<String, Object>> deployConversation(
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal User user) {
        Long conversationId = body.get("conversationId");
        if (conversationId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "conversationId 不能为空"));
        }
        try {
            Map<String, String> result = deployService.deployConversation(conversationId);
            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "data", result,
                    "message", "部署成功"
            ));
        } catch (Exception e) {
            log.error("部署失败: conversationId={}", conversationId, e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "部署失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 从应用 ID 部署代码到 Nginx 预览目录。
     * 优先使用关联对话的 project_files，否则从 sourceCode 解析。
     */
    @PostMapping("/deploy-by-app")
    public ResponseEntity<Map<String, Object>> deployByAppId(
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal User user) {
        Long appId = body.get("appId");
        if (appId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "appId 不能为空"));
        }
        try {
            Map<String, String> result = deployService.deployByAppId(appId);
            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "data", result,
                    "message", "部署成功"
            ));
        } catch (Exception e) {
            log.error("部署失败: appId={}", appId, e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "部署失败: " + e.getMessage()
            ));
        }
    }
}
