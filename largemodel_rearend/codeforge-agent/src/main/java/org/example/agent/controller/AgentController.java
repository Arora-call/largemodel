/**
 * Agent 工作流控制器
 * 功能：工作流 CRUD + SSE 流式执行
 * 作者：yx
 * 创建时间：2026-06-20
 */
package org.example.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.agent.service.AgentService;
import org.example.dto.response.ApiResponse;
import org.example.entity.AgentWorkflow;
import org.example.entity.User;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    private final AgentService agentService;

    /** 创建工作流 */
    @PostMapping("/workflow")
    public ApiResponse<AgentWorkflow> create(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        String name = body.getOrDefault("name", "未命名工作流");
        String desc = body.getOrDefault("description", "");
        String chain = body.getOrDefault("agentChain", "analyzer,architect,coder,tester,reviewer");
        String req = body.getOrDefault("requirement", "");
        return ApiResponse.success(agentService.create(name, desc, chain, req, user.getId()));
    }

    /** 工作流列表 */
    @GetMapping("/workflow")
    public ApiResponse<IPage<AgentWorkflow>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(agentService.listByUser(user.getId(), page, size));
    }

    /** 工作流详情 */
    @GetMapping("/workflow/{id}")
    public ApiResponse<AgentWorkflow> get(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ApiResponse.success(agentService.getById(id, user.getId()));
    }

    /** 更新工作流 */
    @PutMapping("/workflow/{id}")
    public ApiResponse<Void> update(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        agentService.update(id, user.getId(),
                body.get("name"), body.get("description"));
        return ApiResponse.success(null);
    }

    /** 删除工作流 */
    @DeleteMapping("/workflow/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        agentService.delete(id, user.getId());
        return ApiResponse.success(null);
    }

    /** 执行工作流 — SSE 流式 */
    @PostMapping(value = "/workflow/{id}/execute", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter execute(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return agentService.execute(id, user.getId());
    }

    /** 查询任务结果（前端轮询/刷新用） */
    @GetMapping("/tasks/{id}")
    public ApiResponse<AgentWorkflow> getTask(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ApiResponse.success(agentService.getById(id, user.getId()));
    }
}
