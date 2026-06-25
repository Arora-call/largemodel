/**
 * 知识库控制器
 * 功能：文档上传/列表/搜索/删除/下载 + 集合管理
 * 作者：yx
 */
package org.example.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.response.ApiResponse;
import org.example.entity.KnowledgeDocument;
import org.example.entity.User;
import org.example.knowledge.service.KnowledgeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    /** 上传文档 */
    @PostMapping("/documents")
    public ApiResponse<KnowledgeDocument> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "collection", defaultValue = "default") String collection,
            @AuthenticationPrincipal User user) {
        if (file.isEmpty()) return ApiResponse.error("文件不能为空");
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) return ApiResponse.error("文件大小不能超过 10MB");
        KnowledgeDocument doc = knowledgeService.upload(file, user.getId(), collection);
        return ApiResponse.success(doc);
    }

    /** 文档列表 */
    @GetMapping("/documents")
    public ApiResponse<IPage<KnowledgeDocument>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String collection) {
        if (collection != null && !collection.isBlank()) {
            // 按集合筛选 → 用搜索模拟
            return ApiResponse.success(knowledgeService.search(user.getId(), "", page, size));
        }
        return ApiResponse.success(knowledgeService.listByUser(user.getId(), page, size));
    }

    /** 文档详情 */
    @GetMapping("/documents/{id}")
    public ApiResponse<KnowledgeDocument> get(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ApiResponse.success(knowledgeService.getById(id, user.getId()));
    }

    /** 下载文档 */
    @GetMapping("/documents/{id}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        KnowledgeDocument doc = knowledgeService.getById(id, user.getId());
        String text = doc.getContent();
        if (text == null || text.isBlank()) {
            text = doc.getSummary() != null ? doc.getSummary() : "";
        }
        byte[] content = text.getBytes(StandardCharsets.UTF_8);
        String filename = doc.getFileName() != null ? doc.getFileName() : doc.getTitle() + ".txt";
        String encoded = java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }

    /** 删除文档 */
    @DeleteMapping("/documents/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        knowledgeService.delete(id, user.getId());
        return ApiResponse.success(null);
    }

    /** 语义搜索 */
    @PostMapping("/search")
    public ApiResponse<List<KnowledgeDocument>> search(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User user) {
        String query = (String) body.getOrDefault("query", "");
        int topK = body.get("topK") instanceof Number n ? n.intValue() : 5;
        if (query.isBlank()) return ApiResponse.error("搜索关键词不能为空");
        return ApiResponse.success(knowledgeService.semanticSearch(user.getId(), query, topK));
    }

    /** 统计信息 */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats(@AuthenticationPrincipal User user) {
        return ApiResponse.success(knowledgeService.getStats(user.getId()));
    }

    // ─── 集合管理 ───

    /** 集合列表 */
    @GetMapping("/collections")
    public ApiResponse<List<String>> listCollections(@AuthenticationPrincipal User user) {
        return ApiResponse.success(knowledgeService.listCollections(user.getId()));
    }

    /** 删除集合 */
    @DeleteMapping("/collections/{name}")
    public ApiResponse<Void> deleteCollection(
            @PathVariable String name,
            @AuthenticationPrincipal User user) {
        knowledgeService.deleteCollection(name, user.getId());
        return ApiResponse.success(null);
    }
}
