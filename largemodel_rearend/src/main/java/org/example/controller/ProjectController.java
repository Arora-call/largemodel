/**
 * 模块：工程项目
 * 功能：项目控制器 — 流式生成 + 真实文件系统操作
 * 作者：yx
 * 创建时间：2026-06-18
 */
package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.GenerateCodeRequest;
import org.example.dto.response.ApiResponse;
import org.example.entity.Application;
import org.example.entity.User;
import org.example.repository.ApplicationRepository;
import org.example.service.ProjectService;
import org.example.service.ai.AiCodeGenService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final AiCodeGenService aiCodeGenService;
    private final ProjectService projectService;
    private final ApplicationRepository appRepo;

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

    /** 保存项目到应用库（在 AI 生成完成后调用） */
    @PostMapping("/{id}/save")
    public ApiResponse<Void> save(@PathVariable Long id,
                                   @RequestParam String name,
                                   @RequestParam String type,
                                   @AuthenticationPrincipal User user) {
        List<Map<String, Object>> tree = projectService.getFileTree(id);
        Application app = appRepo.findById(id).orElse(Application.builder()
                .userId(user.getId()).type("ENGINEERING").status(2).build());
        app.setName(name);
        app.setLanguage(type);
        app.setSourceCode(""); // 代码在磁盘上
        app.setConfigJson(toJson(tree));
        appRepo.save(app);
        return ApiResponse.msg("项目已保存");
    }

    private String toJson(Object obj) {
        try { return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }
}
