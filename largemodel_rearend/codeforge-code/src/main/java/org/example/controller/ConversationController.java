/**
 * 模块：对话管理
 * 功能：对话控制器，处理对话列表、消息加载、删除等接口
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.ApiResponse;
import org.example.entity.Conversation;
import org.example.entity.Message;
import org.example.entity.User;
import org.example.repository.ConversationRepository;
import org.example.repository.MessageRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
}
