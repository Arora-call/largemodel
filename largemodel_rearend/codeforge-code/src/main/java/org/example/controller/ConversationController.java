/**
 * 模块：对话管理
 * 功能：对话控制器，处理对话列表、消息加载、删除等接口
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.dto.response.ApiResponse;
import org.example.entity.Conversation;
import org.example.entity.Message;
import org.example.entity.User;
import org.example.repository.ConversationRepository;
import org.example.repository.MessageRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationRepository convRepo;
    private final MessageRepository msgRepo;

    /** 对话列表，可按 type 过滤 (NATIVE / ENGINEERING) */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@AuthenticationPrincipal User user,
                                                        @RequestParam(required = false) String type) {
        List<Conversation> convs;
        if (type != null && !type.isBlank()) {
            convs = convRepo.findByUserIdAndStatusAndTypeOrderByUpdatedAtDesc(user.getId(), 1, type);
        } else {
            convs = convRepo.findByUserIdAndStatusOrderByUpdatedAtDesc(user.getId(), 1);
        }
        List<Map<String, Object>> result = convs.stream().map(c -> Map.<String, Object>of(
                "id", c.getId(),
                "title", c.getTitle() != null ? c.getTitle() : "新对话",
                "model", c.getModel() != null ? c.getModel() : "",
                "type", c.getType() != null ? c.getType() : "NATIVE",
                "updatedAt", c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : ""
        )).toList();
        return ApiResponse.success(result);
    }

    /** 对话消息 */
    @GetMapping("/{id}/messages")
    public ApiResponse<List<Map<String, Object>>> messages(@PathVariable Long id,
                                                            @AuthenticationPrincipal User user) {
        List<Message> msgs = msgRepo.findByConversationIdOrderByCreatedAtAsc(id);
        List<Map<String, Object>> result = msgs.stream().map(m -> Map.<String, Object>of(
                "id", m.getId(),
                "role", m.getRole(),
                "content", m.getContent() != null ? m.getContent() : "",
                "createdAt", m.getCreatedAt() != null ? m.getCreatedAt().toString() : ""
        )).toList();
        return ApiResponse.success(result);
    }

    /** 删除对话 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        Conversation c = convRepo.findById(id).orElse(null);
        if (c != null) {
            c.setStatus(0);
            convRepo.save(c);
        }
        return ApiResponse.msg("已删除");
    }

    /** 清空所有对话 */
    @DeleteMapping("/clear")
    public ApiResponse<Void> clearAll(@AuthenticationPrincipal User user) {
        convRepo.deactivateByUserId(user.getId());
        return ApiResponse.msg("已清空");
    }

    /** 导出对话为 Markdown 文件 */
    @GetMapping("/{id}/export")
    public void export(@PathVariable Long id,
                       @AuthenticationPrincipal User user,
                       HttpServletResponse response) throws IOException {
        // 权限校验
        Conversation conv = convRepo.findById(id).orElse(null);
        if (conv == null || !conv.getUserId().equals(user.getId())) {
            response.sendError(403, "无权访问此对话");
            return;
        }
        List<Message> msgs = msgRepo.findByConversationIdOrderByCreatedAtAsc(id);
        String title = conv.getTitle() != null ? conv.getTitle() : "对话记录";
        String model = conv.getModel() != null ? conv.getModel() : "AI";
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        String filename = "conversation_" + id + ".md";
        response.setContentType("text/markdown; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"");

        PrintWriter w = response.getWriter();
        w.println("# " + escapeMarkdown(title));
        w.println("> 导出时间: " + date + " | 模型: " + model);
        w.println();

        for (Message m : msgs) {
            String role = m.getRole();
            String content = m.getContent() != null ? m.getContent() : "";
            if ("USER".equalsIgnoreCase(role)) {
                w.println("## 用户");
                w.println();
                w.println(content);
            } else {
                w.println("## AI");
                w.println();
                w.println(content);
            }
            w.println();
            w.println("---");
            w.println();
        }
        w.flush();
    }

    private String escapeMarkdown(String text) {
        if (text == null) return "";
        // Markdown 标题中不能有 #
        return text.replace("#", "&#35;");
    }
}
