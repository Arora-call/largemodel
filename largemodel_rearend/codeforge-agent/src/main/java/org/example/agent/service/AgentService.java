/**
 * Agent 工作流服务
 * 功能：工作流 CRUD + 顺序链式执行 + SSE 进度推送
 * 作者：yx
 * 创建时间：2026-06-20
 */
package org.example.agent.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.agent.mapper.AgentWorkflowMapper;
import org.example.entity.AgentWorkflow;
import org.example.service.ai.DynamicModelProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final AgentWorkflowMapper workflowMapper;
    private final DynamicModelProvider modelProvider;

    /** Agent 角色定义 */
    private static final Map<String, String> AGENT_ROLES = Map.of(
            "analyzer", "你是资深需求分析师。将用户需求拆解为清晰的功能规格说明。输出要点列表，每点一行。",
            "architect", "你是资深系统架构师。根据需求设计系统架构、模块划分和技术选型。输出架构设计文档，包含：1)整体架构 2)核心模块 3)数据流 4)技术栈建议。",
            "coder", "你是资深全栈工程师。根据架构设计生成完整的可运行代码。输出完整代码，使用代码块包裹。",
            "tester", "你是资深测试工程师。为上述代码生成单元测试和集成测试。输出测试代码，使用代码块包裹。",
            "reviewer", "你是资深代码审查专家。审查上述所有产出，给出改进建议和最终评分(1-10)。"
    );

    /** 创建工作流 */
    @Transactional
    public AgentWorkflow create(String name, String description, String agentChain, String requirement, Long userId) {
        AgentWorkflow wf = AgentWorkflow.builder()
                .name(name)
                .description(description)
                .agentChain(agentChain)
                .requirement(requirement)
                .status("PENDING")
                .userId(userId)
                .build();
        workflowMapper.insert(wf);
        log.info("创建工作流, id={}, name={}, chain={}", wf.getId(), name, agentChain);
        return wf;
    }

    /** 工作流列表 */
    public IPage<AgentWorkflow> listByUser(Long userId, int page, int size) {
        return workflowMapper.findByUserId(Page.of(page, size), userId);
    }

    /** 工作流详情 */
    public AgentWorkflow getById(Long id, Long userId) {
        AgentWorkflow wf = workflowMapper.selectById(id);
        if (wf == null || !wf.getUserId().equals(userId)) {
            throw new RuntimeException("工作流不存在或无权访问");
        }
        return wf;
    }

    /** 删除工作流 */
    @Transactional
    public void delete(Long id, Long userId) {
        getById(id, userId);
        workflowMapper.deleteById(id);
    }

    /** 执行工作流 — SSE 流式返回各阶段进度 */
    public SseEmitter execute(Long workflowId, Long userId) {
        AgentWorkflow wf = getById(workflowId, userId);
        String[] agents = wf.getAgentChain().split(",");
        SseEmitter emitter = new SseEmitter(600_000L); // 10 min timeout

        wf.setStatus("RUNNING");
        workflowMapper.updateById(wf);

        StringBuilder fullResult = new StringBuilder();
        AtomicBoolean failed = new AtomicBoolean(false);

        Thread.ofVirtual().start(() -> {
            try {
                // Use array to allow mutation inside lambda
                String[] context = { wf.getRequirement() };
                for (int i = 0; i < agents.length; i++) {
                    if (failed.get()) break;
                    final int phaseNum = i + 1;
                    final String agent = agents[i].trim();
                    final String rolePrompt = AGENT_ROLES.getOrDefault(agent,
                            "你是 AI 助手。根据上下文任务执行你的职责。");

                    // 发送阶段开始事件
                    Map<String, Object> phase = Map.of(
                            "phase", phaseNum, "total", agents.length,
                            "agent", agent, "status", "running"
                    );
                    emitter.send(SseEmitter.event().name("phase").data(MAPPER.writeValueAsString(phase)));

                    // 构建消息
                    List<ChatMessage> messages = new ArrayList<>();
                    messages.add(SystemMessage.from(rolePrompt));
                    messages.add(UserMessage.from("任务上下文：\n" + context[0] + "\n\n请执行你的职责并输出结果。"));

                    // 流式执行当前 Agent
                    StringBuilder agentOutput = new StringBuilder();
                    AtomicBoolean agentDone = new AtomicBoolean(false);

                    StreamingChatLanguageModel model = modelProvider.getDefaultStreaming();
                    model.chat(messages, new StreamingChatResponseHandler() {
                        @Override public void onPartialResponse(String token) {
                            if (agentDone.get() || failed.get()) return;
                            agentOutput.append(token);
                            try {
                                Map<String, Object> progress = Map.of(
                                        "phase", phaseNum, "agent", agent, "token", token
                                );
                                emitter.send(SseEmitter.event().name("progress").data(MAPPER.writeValueAsString(progress)));
                            } catch (IOException e) { failed.set(true); }
                        }

                        @Override public void onCompleteResponse(ChatResponse response) {
                            agentDone.set(true);
                        }

                        @Override public void onError(Throwable error) {
                            agentDone.set(true);
                            failed.set(true);
                            try {
                                Map<String, Object> err = Map.of("phase", phaseNum, "agent", agent, "error", error.getMessage());
                                emitter.send(SseEmitter.event().name("error").data(MAPPER.writeValueAsString(err)));
                            } catch (IOException ignored) {}
                        }
                    });

                    // Wait for agent to complete (simplified: polling)
                    while (!agentDone.get() && !failed.get()) {
                        try { Thread.sleep(100); } catch (InterruptedException e) { break; }
                    }

                    if (failed.get()) break;
                    context[0] += "\n\n===== " + agent + " 输出 =====\n" + agentOutput;
                    fullResult.append("## ").append(agent).append("\n\n").append(agentOutput).append("\n\n");
                }

                // 更新工作流状态
                wf.setStatus(failed.get() ? "FAILED" : "COMPLETED");
                wf.setResult(fullResult.toString());
                workflowMapper.updateById(wf);

                Map<String, Object> done = Map.of(
                        "workflowId", wf.getId(),
                        "status", wf.getStatus(),
                        "result", fullResult.toString()
                );
                emitter.send(SseEmitter.event().name("done").data(MAPPER.writeValueAsString(done)));
                emitter.complete();

            } catch (Exception e) {
                log.error("工作流执行失败", e);
                wf.setStatus("FAILED");
                wf.setResult(e.getMessage());
                workflowMapper.updateById(wf);
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> {
            wf.setStatus("FAILED");
            workflowMapper.updateById(wf);
            emitter.complete();
        });
        return emitter;
    }
}
