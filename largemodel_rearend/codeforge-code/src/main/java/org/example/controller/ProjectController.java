/**
 * 模块：工程项目
 * 功能：项目控制器 — 流式生成 + 真实文件系统操作
 * 作者：yx
 * 创建时间：2026-06-18
 */
package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.GenerateCodeRequest;
import org.example.dto.response.ApiResponse;
import org.example.entity.Application;
import org.example.entity.Conversation;
import org.example.entity.User;
import org.example.repository.ApplicationRepository;
import org.example.repository.ConversationRepository;
import org.example.repository.MessageRepository;
import org.example.service.ProjectService;
import org.example.service.ai.AiCodeGenService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final AiCodeGenService aiCodeGenService;
    private final ProjectService projectService;
    private final ApplicationRepository appRepo;
    private final ConversationRepository convRepo;
    private final MessageRepository msgRepo;

    /** 流式生成工程项目 — 完成后自动在磁盘创建真实文件 */
    @PostMapping("/generate")
    public SseEmitter generate(@RequestBody GenerateCodeRequest request,
                                @AuthenticationPrincipal User user) {
        request.setType("ENGINEERING");
        return aiCodeGenService.generateProject(request, user.getId());
    }

    /** 获取项目文件树 */
    @GetMapping("/{id}/tree")
    public ApiResponse<List<Map<String, Object>>> tree(@PathVariable Long id) {
        return ApiResponse.success(projectService.getFileTree(id));
    }

    /** 读取项目中的单个文件 */
    @GetMapping("/{id}/file")
    public ApiResponse<Map<String, String>> readFile(@PathVariable Long id,
                                                      @RequestParam String path) {
        String content = projectService.readFile(id, path);
        return ApiResponse.success(Map.of("path", path, "content", content));
    }

    /** 下载项目 ZIP */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        var zipPath = projectService.createZip(id);
        Resource resource = new FileSystemResource(zipPath);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"project_" + id + ".zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /** 删除项目 — 先删数据库记录，再清磁盘文件 */
    @Transactional
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        msgRepo.deleteByConversationId(id);
        convRepo.findById(id).ifPresent(c -> {
            c.setStatus(0);
            convRepo.save(c);
        });
        projectService.deleteProject(id);
        return ApiResponse.msg("项目已删除");
    }

    /** AI 修改后保存文件到磁盘 */
    @PostMapping("/{id}/files")
    public ApiResponse<List<Map<String, Object>>> updateFiles(@PathVariable Long id,
                                                               @RequestBody Map<String, String> filesMap) {
        List<Map<String, Object>> tree = projectService.updateFiles(id, filesMap);
        return ApiResponse.success(tree);
    }

    /** 保存项目到应用库（在 AI 生成完成后调用） */
    @PostMapping("/{id}/save")
    public ApiResponse<Void> save(@PathVariable Long id,
                                   @RequestParam String name,
                                   @RequestParam String type,
                                   @AuthenticationPrincipal User user) {
        List<Map<String, Object>> tree = projectService.getFileTree(id);

        // 读取所有文件内容拼接为 sourceCode（前端详情预览用）
        StringBuilder codeBuilder = new StringBuilder();
        for (Map<String, Object> file : tree) {
            String path = (String) file.get("path");
            if (path != null) {
                try {
                    String content = projectService.readFile(id, path);
                    if (!codeBuilder.isEmpty()) codeBuilder.append("\n\n");
                    codeBuilder.append("// ===== ").append(path).append(" =====\n").append(content);
                } catch (Exception ignored) {}
            }
        }
        String sourceCode = codeBuilder.toString();
        // 截断：数据库字段可能有限，保留前 50000 字符足够预览
        if (sourceCode.length() > 50000) {
            sourceCode = sourceCode.substring(0, 50000) + "\n\n// ... 代码过长已截断";
        }

        Application app = appRepo.findById(id).orElse(Application.builder()
                .userId(user.getId()).type("ENGINEERING").status(2).build());
        app.setName(name);
        app.setLanguage(type);
        app.setSourceCode(sourceCode);
        // configJson: 存文件路径列表 + 文件树，方便前端展示项目结构
        Map<String, Object> config = new HashMap<>();
        config.put("files", tree.stream().map(f -> f.get("path")).toList());
        config.put("tree", tree);
        app.setConfigJson(toJson(config));
        appRepo.save(app);
        return ApiResponse.msg("项目已保存");
    }

    private String toJson(Object obj) {
        try { return MAPPER.writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }
}
